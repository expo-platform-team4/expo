package com.expo.booth.service;

import com.expo.booth.converter.BoothOrderConverter;
import com.expo.booth.dto.BoothOrderExpirationResult;
import com.expo.booth.dto.BoothOrderResponse;
import com.expo.booth.entity.BoothOrder;
import com.expo.booth.entity.BoothOrderStatus;
import com.expo.booth.entity.BoothProduct;
import com.expo.booth.entity.BoothReservation;
import com.expo.booth.entity.BoothReservationStatus;
import com.expo.booth.entity.BoothSalesStatus;
import com.expo.booth.repository.BoothOrderRepository;
import com.expo.booth.repository.BoothProductRepository;
import com.expo.booth.repository.BoothReservationRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.participation.entity.ParticipationApplication;
import com.expo.participation.entity.ParticipationApplicationStatus;
import com.expo.participation.repository.ParticipationApplicationRepository;
import com.expo.recruitment.entity.RecruitmentNoticeStatus;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 참여 신청서에서 선택한 부스 상품의 주문 생성·취소. 결제(토스페이먼츠) 연동 전 단계. */
@Slf4j
@Service
public class BoothOrderService {

    private static final int ORDER_EXPIRATION_MINUTES = 15;

    private final BoothOrderRepository boothOrderRepository;
    private final BoothReservationRepository boothReservationRepository;
    private final BoothProductRepository boothProductRepository;
    private final ParticipationApplicationRepository participationApplicationRepository;
    private final RecruitmentNoticeRepository recruitmentNoticeRepository;
    private final BoothOrderConverter boothOrderConverter;

    public BoothOrderService(
            BoothOrderRepository boothOrderRepository,
            BoothReservationRepository boothReservationRepository,
            BoothProductRepository boothProductRepository,
            ParticipationApplicationRepository participationApplicationRepository,
            RecruitmentNoticeRepository recruitmentNoticeRepository,
            BoothOrderConverter boothOrderConverter) {
        this.boothOrderRepository = boothOrderRepository;
        this.boothReservationRepository = boothReservationRepository;
        this.boothProductRepository = boothProductRepository;
        this.participationApplicationRepository = participationApplicationRepository;
        this.recruitmentNoticeRepository = recruitmentNoticeRepository;
        this.boothOrderConverter = boothOrderConverter;
    }

    /**
     * 부스 상품 주문 생성. 신청서가 초안 상태이고 부스 상품을 선택해뒀어야 하며, 그 상품이 구매 가능한 상태여야 한다. 생성과 동시에 부스
     * 상품을 임시 확보한다.
     *
     * <p>신청서 작성 이후 공고가 조기 마감·취소됐을 수 있어, 신청서 상태만으로는 걸러지지 않는다 - 여기서 공고가 아직
     * 게시(OPEN) 중인지 다시 확인한다.
     */
    @Transactional
    public BoothOrderResponse create(Long applicationId, Long clientUserId) {
        ParticipationApplication application =
                participationApplicationRepository
                        .findByIdAndClientUserId(applicationId, clientUserId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.PARTICIPATION_APPLICATION_NOT_FOUND));
        if (application.getStatus() != ParticipationApplicationStatus.DRAFT) {
            throw new BusinessException(ErrorCode.BOOTH_ORDER_NOT_ALLOWED);
        }
        boolean noticeOpen =
                recruitmentNoticeRepository
                        .findById(application.getRecruitmentNoticeId())
                        .map(notice -> notice.getStatus() == RecruitmentNoticeStatus.OPEN)
                        .orElse(false);
        if (!noticeOpen) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_OPEN);
        }
        Long boothProductId = application.getSelectedBoothProductId();
        if (boothProductId == null) {
            throw new BusinessException(ErrorCode.BOOTH_PRODUCT_NOT_SELECTED);
        }
        BoothProduct product =
                boothProductRepository
                        .findById(boothProductId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.BOOTH_PRODUCT_NOT_FOUND));
        if (product.getSalesStatus() != BoothSalesStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.BOOTH_PRODUCT_NOT_AVAILABLE);
        }
        Instant now = Instant.now();
        if ((product.getSalesStartAt() != null && now.isBefore(product.getSalesStartAt()))
                || (product.getSalesEndAt() != null && now.isAfter(product.getSalesEndAt()))) {
            throw new BusinessException(ErrorCode.BOOTH_PRODUCT_SALES_NOT_OPEN);
        }

        Instant expiresAt = now.plus(Duration.ofMinutes(ORDER_EXPIRATION_MINUTES));
        BoothOrder order =
                BoothOrder.create(
                        applicationId,
                        clientUserId,
                        boothProductId,
                        generateOrderNumber(),
                        product.getTotalPrice(),
                        UUID.randomUUID().toString(),
                        expiresAt);
        BoothOrder savedOrder;
        try {
            savedOrder = boothOrderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException e) {
            log.warn(
                    "부스 주문 저장 중 예상하지 못한 무결성 제약 위반. applicationId={}, boothProductId={}",
                    applicationId,
                    boothProductId,
                    e);
            throw e;
        }

        product.reserve();
        try {
            boothReservationRepository.saveAndFlush(
                    BoothReservation.create(
                            boothProductId, savedOrder.getId(), clientUserId, expiresAt));
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("uq_booth_reservations_active")) {
                throw new BusinessException(ErrorCode.BOOTH_PRODUCT_NOT_AVAILABLE);
            }
            log.warn(
                    "부스 임시 확보 저장 중 예상하지 못한 무결성 제약 위반. boothProductId={}, boothOrderId={}",
                    boothProductId,
                    savedOrder.getId(),
                    e);
            throw e;
        }

        application.startPayment(savedOrder.getId());
        return boothOrderConverter.toResponse(savedOrder);
    }

    /** 주문 상세 조회 (본인). */
    @Transactional(readOnly = true)
    public BoothOrderResponse getMine(Long orderId, Long clientUserId) {
        return boothOrderConverter.toResponse(getOwnedEntity(orderId, clientUserId));
    }

    /**
     * 결제 전 주문 취소. 임시 확보를 풀고 부스 상품을 다시 구매 가능하게 되돌린다.
     *
     * <p>이 주문이 실제로 갖고 있던 활성 예약을 찾아 해제했을 때만 부스 상품을 되돌린다. 상품 상태만 보고 되돌리면, 이 주문의
     * 예약이 이미 만료·해제된 뒤 다른 주문이 같은 상품을 새로 예약한 경우 그 새 예약을 엉뚱하게 풀어버릴 수 있다.
     */
    @Transactional
    public BoothOrderResponse cancel(Long orderId, Long clientUserId) {
        BoothOrder order = getOwnedEntity(orderId, clientUserId);
        if (order.getStatus() != BoothOrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.BOOTH_ORDER_NOT_CANCELABLE);
        }
        order.cancel();
        releaseReservationAndRevertProduct(orderId, order);
        participationApplicationRepository
                .findByIdAndClientUserId(order.getApplicationId(), clientUserId)
                .ifPresent(ParticipationApplication::cancelPayment);

        return boothOrderConverter.toResponse(order);
    }

    /**
     * 결제 대기 시간(15분)을 넘긴 주문을 일괄 만료 처리한다. 취소와 마찬가지로 임시 확보를 풀고 부스 상품·신청서를 되돌리지만,
     * 본인 확인 없이 시스템이 만료 시각만 보고 처리한다는 점이 다르다.
     *
     * <p>대상을 고르는 조회는 잠금 없이 하지만, 실제로 만료 처리하기 직전에 {@link
     * BoothOrderRepository#findByIdForUpdate} 로 다시 잠그고 상태를 재확인한다 - 그렇지 않으면 결제 승인
     * ({@code BoothPaymentService.confirm()})과 경합해서, 방금 결제가 확정된 주문을 만료로 덮어쓰거나 예약을
     * 엉뚱하게 풀어버릴 수 있다. {@code confirm()} 도 같은 행 잠금을 쓰므로 둘 중 하나가 끝날 때까지 다른 하나는
     * 기다렸다가 바뀐 상태를 다시 보고 판단하게 된다.
     *
     * <p>여러 번 호출해도 안전하다 — 이미 처리된 주문은 더 이상 {@code PENDING_PAYMENT} 가 아니므로 다시 조회되지
     * 않는다. 다중 인스턴스에서의 중복 실행 방지 장치가 없어 아직 스케줄러는 붙이지 않았다({@code
     * InternalSettlementController} 와 동일한 판단).
     *
     * <p>주문 ID 오름차순으로 정렬한 뒤 순서대로 잠근다 - 겹치는 두 배치가 동시에 돌면(예: 관리자가 두 번 눌렀거나,
     * 스케줄러가 붙은 뒤 이전 호출이 아직 안 끝났는데 다음 호출이 시작된 경우) 서로 반대 순서로 잠그다가 교착 상태에
     * 빠질 수 있다.
     */
    @Transactional
    public BoothOrderExpirationResult expireDue() {
        List<Long> dueOrderIds =
                boothOrderRepository
                        .findAllByStatusAndExpiresAtBefore(
                                BoothOrderStatus.PENDING_PAYMENT, Instant.now())
                        .stream()
                        .map(BoothOrder::getId)
                        .sorted()
                        .toList();
        List<BoothOrderExpirationResult.Expired> expired =
                dueOrderIds.stream().flatMap(id -> expireOne(id).stream()).toList();
        return new BoothOrderExpirationResult(expired.size(), expired);
    }

    private Optional<BoothOrderExpirationResult.Expired> expireOne(Long orderId) {
        Optional<BoothOrder> locked = boothOrderRepository.findByIdForUpdate(orderId);
        if (locked.isEmpty() || locked.get().getStatus() != BoothOrderStatus.PENDING_PAYMENT) {
            return Optional.empty();
        }
        BoothOrder order = locked.get();
        order.expire();
        releaseReservationAndRevertProduct(orderId, order);
        participationApplicationRepository
                .findById(order.getApplicationId())
                .ifPresent(ParticipationApplication::cancelPayment);
        return Optional.of(
                new BoothOrderExpirationResult.Expired(
                        order.getId(), order.getOrderNumber(), order.getApplicationId()));
    }

    /**
     * 이 주문이 실제로 갖고 있던 활성 예약을 찾아 해제했을 때만 부스 상품을 되돌린다. 상품 상태만 보고 되돌리면, 이 주문의 예약이
     * 이미 만료·해제된 뒤 다른 주문이 같은 상품을 새로 예약한 경우 그 새 예약을 엉뚱하게 풀어버릴 수 있다.
     */
    private void releaseReservationAndRevertProduct(Long orderId, BoothOrder order) {
        boolean releasedOwnReservation =
                boothReservationRepository
                        .findFirstByBoothOrderIdAndStatus(orderId, BoothReservationStatus.ACTIVE)
                        .map(
                                reservation -> {
                                    reservation.release();
                                    return true;
                                })
                        .orElse(false);

        if (releasedOwnReservation) {
            boothProductRepository
                    .findById(order.getBoothProductId())
                    .filter(product -> product.getSalesStatus() == BoothSalesStatus.RESERVED)
                    .ifPresent(BoothProduct::cancelReservation);
        }
    }

    private BoothOrder getOwnedEntity(Long orderId, Long clientUserId) {
        return boothOrderRepository
                .findByIdAndClientUserId(orderId, clientUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_ORDER_NOT_FOUND));
    }

    private String generateOrderNumber() {
        return "BO" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}
