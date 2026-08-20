package com.expo.booth.service;

import com.expo.booth.converter.BoothAllocationConverter;
import com.expo.booth.dto.BoothAllocationResponse;
import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.entity.BoothAllocationStatus;
import com.expo.booth.entity.BoothContent;
import com.expo.booth.entity.BoothContentStatus;
import com.expo.booth.entity.BoothManagementActionType;
import com.expo.booth.entity.BoothManagementHistory;
import com.expo.booth.entity.BoothProduct;
import com.expo.booth.entity.BoothSalesStatus;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothContentRepository;
import com.expo.booth.repository.BoothManagementHistoryRepository;
import com.expo.booth.repository.BoothProductRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 부스 확정 배정 조회·취소·재배정. 배정 생성 자체는 결제 승인 흐름({@link BoothPaymentService})이 담당한다. */
@Slf4j
@Service
public class BoothAllocationService {

    private final BoothAllocationRepository boothAllocationRepository;
    private final BoothContentRepository boothContentRepository;
    private final BoothProductRepository boothProductRepository;
    private final BoothManagementHistoryRepository boothManagementHistoryRepository;
    private final BoothAllocationConverter boothAllocationConverter;

    public BoothAllocationService(
            BoothAllocationRepository boothAllocationRepository,
            BoothContentRepository boothContentRepository,
            BoothProductRepository boothProductRepository,
            BoothManagementHistoryRepository boothManagementHistoryRepository,
            BoothAllocationConverter boothAllocationConverter) {
        this.boothAllocationRepository = boothAllocationRepository;
        this.boothContentRepository = boothContentRepository;
        this.boothProductRepository = boothProductRepository;
        this.boothManagementHistoryRepository = boothManagementHistoryRepository;
        this.boothAllocationConverter = boothAllocationConverter;
    }

    /** 본인 배정 상세 조회. */
    @Transactional(readOnly = true)
    public BoothAllocationResponse getMine(Long allocationId, Long clientUserId) {
        BoothAllocation allocation =
                boothAllocationRepository
                        .findByIdAndClientUserId(allocationId, clientUserId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND));
        return boothAllocationConverter.toResponse(allocation);
    }

    /** 관리자용 배정 목록 조회 (페이지 단위). */
    @Transactional(readOnly = true)
    public Page<BoothAllocationResponse> listForAdmin(Pageable pageable) {
        return boothAllocationRepository
                .findAll(pageable)
                .map(boothAllocationConverter::toResponse);
    }

    /** 관리자용 배정 상세 조회. */
    @Transactional(readOnly = true)
    public BoothAllocationResponse getForAdmin(Long allocationId) {
        return boothAllocationConverter.toResponse(getEntity(allocationId));
    }

    /**
     * 관리자 배정 취소. 부스 이중 배정 등 운영상 정정이 필요할 때만 쓴다.
     *
     * <p>부스 상품은 되돌리지 않고 {@code SOLD} 로 남긴다. {@code booth_allocations.booth_product_id} 는
     * 전역 UNIQUE 라 상품을 다시 팔 수 있게 하면, 그 상품이 재판매·재결제된 시점에 새 배정 저장이 같은
     * 상품 ID 로 유니크 제약 위반이 난다. 취소된 배정을 재판매로 이어가려면 주문·결제·예약·신청서·배정을
     * 함께 되돌리는 보상 흐름이 먼저 있어야 한다.
     *
     * <p>공개(PUBLISHED) 상태인 콘텐츠가 있으면 같이 숨긴다 - 안 그러면 배정이 사라진 뒤에도 그 회사 소개 페이지가
     * 공개 사이트에 계속 떠 있게 된다.
     *
     * <p>권한이 걸린 변경이라 처리 관리자·사유를 감사 이력({@code booth_management_histories})에 남긴다.
     */
    @Transactional
    public BoothAllocationResponse cancel(Long allocationId, Long adminId, String reason) {
        BoothAllocation allocation =
                boothAllocationRepository
                        .findByIdForUpdate(allocationId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND));
        if (allocation.getStatus() != BoothAllocationStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_CANCELABLE);
        }
        allocation.cancel(reason);
        BoothContent content =
                boothContentRepository.findByBoothAllocationId(allocationId).orElse(null);
        Long boothContentId = content != null ? content.getId() : null;
        boothManagementHistoryRepository.save(
                BoothManagementHistory.create(
                        allocationId,
                        boothContentId,
                        BoothManagementActionType.ALLOCATION_CORRECTED,
                        reason,
                        adminId));
        if (content != null && content.getStatus() == BoothContentStatus.PUBLISHED) {
            content.hide();
            boothManagementHistoryRepository.save(
                    BoothManagementHistory.create(
                            allocationId,
                            boothContentId,
                            BoothManagementActionType.CONTENT_HIDDEN,
                            "배정 취소로 자동 숨김",
                            adminId));
        }
        return boothAllocationConverter.toResponse(allocation);
    }

    /**
     * 관리자 재배정. 이중 배정 등으로 잘못 확정된 배정을 다른(구매 가능한) 부스 상품으로 옮긴다. 취소와 달리 신청서·주문·
     * 클라이언트는 그대로 두고 대상 상품만 바뀌므로, 결제·주문을 다시 거치지 않고 바로 정정할 수 있다.
     *
     * <p>기존 상품은 판매 가능 상태로 되돌리고 새 상품은 판매 완료로 확정한다. 사전 검사를 통과해도 동시에 같은 상품으로
     * 재배정하려는 요청이 있으면 {@code booth_allocations_booth_product_id_key} 유니크 제약이 최종적으로 막는다.
     *
     * <p>권한이 걸린 변경이라 처리 관리자·사유를 감사 이력({@code booth_management_histories})에 남긴다.
     */
    @Transactional
    public BoothAllocationResponse reassign(
            Long allocationId, Long newBoothProductId, Long adminId, String reason) {
        BoothAllocation allocation =
                boothAllocationRepository
                        .findByIdForUpdate(allocationId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND));
        if (allocation.getStatus() != BoothAllocationStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_REASSIGNABLE);
        }
        BoothProduct newProduct =
                boothProductRepository
                        .findById(newBoothProductId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.BOOTH_PRODUCT_NOT_FOUND));
        if (newProduct.getSalesStatus() != BoothSalesStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.BOOTH_PRODUCT_NOT_AVAILABLE);
        }
        Long previousBoothProductId = allocation.getBoothProductId();
        BoothProduct previousProduct =
                boothProductRepository
                        .findById(previousBoothProductId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.BOOTH_PRODUCT_NOT_FOUND));
        allocation.reassign(newBoothProductId);
        try {
            boothAllocationRepository.saveAndFlush(allocation);
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("booth_allocations_booth_product_id_key")) {
                throw new BusinessException(ErrorCode.BOOTH_PRODUCT_NOT_AVAILABLE);
            }
            log.warn("부스 배정 재배정 중 예상하지 못한 무결성 제약 위반. allocationId={}", allocationId, e);
            throw e;
        }
        previousProduct.cancelReservation();
        newProduct.markSold();
        boothManagementHistoryRepository.save(
                BoothManagementHistory.create(
                        allocationId,
                        findBoothContentId(allocationId),
                        BoothManagementActionType.INFORMATION_UPDATED,
                        reason,
                        adminId));
        return boothAllocationConverter.toResponse(allocation);
    }

    private Long findBoothContentId(Long allocationId) {
        return boothContentRepository
                .findByBoothAllocationId(allocationId)
                .map(BoothContent::getId)
                .orElse(null);
    }

    private BoothAllocation getEntity(Long allocationId) {
        return boothAllocationRepository
                .findById(allocationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND));
    }
}
