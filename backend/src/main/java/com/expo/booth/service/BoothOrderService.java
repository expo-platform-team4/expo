package com.expo.booth.service;

import com.expo.booth.converter.BoothOrderConverter;
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
import java.time.LocalDateTime;
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
    private final BoothOrderConverter boothOrderConverter;

    public BoothOrderService(
            BoothOrderRepository boothOrderRepository,
            BoothReservationRepository boothReservationRepository,
            BoothProductRepository boothProductRepository,
            ParticipationApplicationRepository participationApplicationRepository,
            BoothOrderConverter boothOrderConverter) {
        this.boothOrderRepository = boothOrderRepository;
        this.boothReservationRepository = boothReservationRepository;
        this.boothProductRepository = boothProductRepository;
        this.participationApplicationRepository = participationApplicationRepository;
        this.boothOrderConverter = boothOrderConverter;
    }

    /**
     * 부스 상품 주문 생성. 신청서가 초안 상태이고 부스 상품을 선택해뒀어야 하며, 그 상품이 구매 가능한 상태여야 한다. 생성과 동시에 부스
     * 상품을 임시 확보한다.
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
        LocalDateTime now = LocalDateTime.now();
        if ((product.getSalesStartAt() != null && now.isBefore(product.getSalesStartAt()))
                || (product.getSalesEndAt() != null && now.isAfter(product.getSalesEndAt()))) {
            throw new BusinessException(ErrorCode.BOOTH_PRODUCT_SALES_NOT_OPEN);
        }

        LocalDateTime expiresAt = now.plusMinutes(ORDER_EXPIRATION_MINUTES);
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

        participationApplicationRepository
                .findByIdAndClientUserId(order.getApplicationId(), clientUserId)
                .ifPresent(ParticipationApplication::cancelPayment);

        return boothOrderConverter.toResponse(order);
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
