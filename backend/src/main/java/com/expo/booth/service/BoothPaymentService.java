package com.expo.booth.service;

import com.expo.booth.converter.BoothPaymentConverter;
import com.expo.booth.dto.BoothPaymentResponse;
import com.expo.booth.dto.InitiateBoothPaymentResponse;
import com.expo.booth.entity.BoothOrder;
import com.expo.booth.entity.BoothOrderStatus;
import com.expo.booth.entity.BoothPayment;
import com.expo.booth.entity.BoothPaymentEventType;
import com.expo.booth.entity.BoothPaymentHistory;
import com.expo.booth.entity.BoothPaymentStatus;
import com.expo.booth.repository.BoothOrderRepository;
import com.expo.booth.repository.BoothPaymentHistoryRepository;
import com.expo.booth.repository.BoothPaymentRepository;
import com.expo.common.config.TossApiException;
import com.expo.common.config.TossConfirmResult;
import com.expo.common.config.TossPaymentClient;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 부스 상품 주문의 토스페이먼츠 결제 시작·승인. */
@Slf4j
@Service
public class BoothPaymentService {

    private final BoothPaymentRepository boothPaymentRepository;
    private final BoothPaymentHistoryRepository boothPaymentHistoryRepository;
    private final BoothOrderRepository boothOrderRepository;
    private final BoothOrderCompletionService boothOrderCompletionService;
    private final TossPaymentClient tossPaymentClient;
    private final BoothPaymentConverter boothPaymentConverter;

    public BoothPaymentService(
            BoothPaymentRepository boothPaymentRepository,
            BoothPaymentHistoryRepository boothPaymentHistoryRepository,
            BoothOrderRepository boothOrderRepository,
            BoothOrderCompletionService boothOrderCompletionService,
            TossPaymentClient tossPaymentClient,
            BoothPaymentConverter boothPaymentConverter) {
        this.boothPaymentRepository = boothPaymentRepository;
        this.boothPaymentHistoryRepository = boothPaymentHistoryRepository;
        this.boothOrderRepository = boothOrderRepository;
        this.boothOrderCompletionService = boothOrderCompletionService;
        this.tossPaymentClient = tossPaymentClient;
        this.boothPaymentConverter = boothPaymentConverter;
    }

    /**
     * 결제 시작. 이미 진행 중인 결제가 있으면 그대로 재사용하고, 없으면 새 결제 시도를 만든다. 주문이 결제 대기 상태가 아니거나
     * 만료됐으면 시작할 수 없다.
     *
     * <p>주문 행에 잠금을 걸어, 동시에 들어온 두 시작 요청이 서로 다른 결제 시도를 중복 생성하지 못하게 막는다.
     */
    @Transactional
    public InitiateBoothPaymentResponse initiate(Long orderId, Long clientUserId) {
        BoothOrder order = getOwnedOrderForUpdate(orderId, clientUserId);
        validateOrderPayable(order);

        List<BoothPayment> existing = boothPaymentRepository.findAllByBoothOrderId(orderId);
        boolean alreadyApproved =
                existing.stream().anyMatch(p -> p.getStatus() == BoothPaymentStatus.APPROVED);
        if (alreadyApproved) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_APPROVED);
        }
        BoothPayment payment =
                existing.stream()
                        .filter(
                                p ->
                                        p.getStatus() == BoothPaymentStatus.READY
                                                || p.getStatus() == BoothPaymentStatus.IN_PROGRESS)
                        .findFirst()
                        .orElseGet(() -> createNewAttempt(order, existing.size()));

        return new InitiateBoothPaymentResponse(
                payment.getId(),
                tossPaymentClient.getClientKey(),
                payment.getPgOrderId(),
                "부스 상품 주문 " + order.getOrderNumber(),
                payment.getRequestedAmount());
    }

    private BoothPayment createNewAttempt(BoothOrder order, int attemptCount) {
        BoothPayment payment =
                BoothPayment.create(
                        order.getId(),
                        order.getOrderNumber() + "-P" + (attemptCount + 1),
                        order.getTotalAmount(),
                        UUID.randomUUID().toString());
        BoothPayment saved = boothPaymentRepository.saveAndFlush(payment);
        boothPaymentHistoryRepository.save(
                BoothPaymentHistory.record(
                        saved.getId(),
                        BoothPaymentEventType.REQUEST,
                        null,
                        BoothPaymentStatus.READY,
                        saved.getRequestedAmount(),
                        null,
                        null));
        return saved;
    }

    /**
     * 토스 결제창에서 돌아온 뒤 결제 승인 요청. 실패해도 주문은 결제 대기 상태로 남아 재시도할 수 있다.
     *
     * <p>주문 행에 잠금을 걸어, 동시에 들어온 두 승인 요청이 둘 다 상태 검사를 통과해 배정을 중복 생성하는 것을 막는다.
     *
     * <p>{@code noRollbackFor}: 토스 승인 실패 시 {@code payment.fail(...)}과 FAIL 이력 저장을 남긴 뒤
     * {@link ErrorCode#PAYMENT_APPROVAL_FAILED} 로 변환해 던진다. 기본 롤백 정책대로면 이 실패 기록 자체가
     * 롤백되어 사라지므로, 여기서는 롤백하지 않는다 — 이 시점까지 발생하는 다른 {@link BusinessException} 은 아직
     * 아무 것도 쓰지 않은 조회 단계에서만 던져지므로 커밋해도 부작용이 없다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public BoothPaymentResponse confirm(
            String pgOrderId, String paymentKey, BigDecimal amount, Long clientUserId) {
        BoothPayment payment =
                boothPaymentRepository
                        .findByPgOrderId(pgOrderId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        BoothOrder order =
                boothOrderRepository
                        .findByIdForUpdate(payment.getBoothOrderId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_ORDER_NOT_FOUND));
        if (!order.getClientUserId().equals(clientUserId)) {
            throw new BusinessException(ErrorCode.BOOTH_ORDER_NOT_FOUND);
        }
        if (payment.getStatus() == BoothPaymentStatus.APPROVED) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_APPROVED);
        }
        if (payment.getStatus() != BoothPaymentStatus.READY
                && payment.getStatus() != BoothPaymentStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_NOT_PENDING);
        }
        validateOrderPayable(order);
        if (payment.getRequestedAmount().compareTo(amount) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        BoothPaymentStatus statusBeforeAttempt = payment.getStatus();
        try {
            TossConfirmResult result =
                    tossPaymentClient.confirmPayment(
                            paymentKey, pgOrderId, amount, payment.getIdempotencyKey());
            payment.approve(result.paymentKey(), result.method(), result.totalAmount());
            boothPaymentHistoryRepository.save(
                    BoothPaymentHistory.record(
                            payment.getId(),
                            BoothPaymentEventType.APPROVE,
                            statusBeforeAttempt,
                            BoothPaymentStatus.APPROVED,
                            result.totalAmount(),
                            result.paymentKey(),
                            result.rawResponse()));
            boothOrderCompletionService.complete(order);
        } catch (TossApiException e) {
            payment.fail(e.getCode());
            boothPaymentHistoryRepository.save(
                    BoothPaymentHistory.record(
                            payment.getId(),
                            BoothPaymentEventType.FAIL,
                            statusBeforeAttempt,
                            BoothPaymentStatus.FAILED,
                            null,
                            null,
                            e.getRawResponse()));
            log.warn(
                    "부스 결제 승인 실패. boothOrderId={}, pgOrderId={}, tossCode={}",
                    order.getId(),
                    pgOrderId,
                    e.getCode(),
                    e);
            throw new BusinessException(ErrorCode.PAYMENT_APPROVAL_FAILED);
        }

        return boothPaymentConverter.toResponse(payment);
    }

    /** 주문별 결제 시도 목록 조회 (본인). */
    @Transactional(readOnly = true)
    public List<BoothPaymentResponse> listMine(Long orderId, Long clientUserId) {
        getOwnedOrder(orderId, clientUserId);
        return boothPaymentRepository.findAllByBoothOrderId(orderId).stream()
                .map(boothPaymentConverter::toResponse)
                .toList();
    }

    private BoothOrder getOwnedOrder(Long orderId, Long clientUserId) {
        return boothOrderRepository
                .findByIdAndClientUserId(orderId, clientUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_ORDER_NOT_FOUND));
    }

    private BoothOrder getOwnedOrderForUpdate(Long orderId, Long clientUserId) {
        BoothOrder order =
                boothOrderRepository
                        .findByIdForUpdate(orderId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_ORDER_NOT_FOUND));
        if (!order.getClientUserId().equals(clientUserId)) {
            throw new BusinessException(ErrorCode.BOOTH_ORDER_NOT_FOUND);
        }
        return order;
    }

    private void validateOrderPayable(BoothOrder order) {
        if (order.getStatus() != BoothOrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_NOT_PENDING);
        }
        if (LocalDateTime.now().isAfter(order.getExpiresAt())) {
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_EXPIRED);
        }
    }
}
