package com.expo.booth.service;

import com.expo.booth.converter.BoothAllocationConverter;
import com.expo.booth.dto.BoothAllocationResponse;
import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.entity.BoothAllocationStatus;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 부스 확정 배정 조회·취소. 배정 생성 자체는 결제 승인 흐름({@link BoothPaymentService})이 담당한다. */
@Service
public class BoothAllocationService {

    private final BoothAllocationRepository boothAllocationRepository;
    private final BoothAllocationConverter boothAllocationConverter;

    public BoothAllocationService(
            BoothAllocationRepository boothAllocationRepository,
            BoothAllocationConverter boothAllocationConverter) {
        this.boothAllocationRepository = boothAllocationRepository;
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
     */
    @Transactional
    public BoothAllocationResponse cancel(Long allocationId, String reason) {
        BoothAllocation allocation =
                boothAllocationRepository
                        .findByIdForUpdate(allocationId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND));
        if (allocation.getStatus() != BoothAllocationStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_CANCELABLE);
        }
        allocation.cancel(reason);
        return boothAllocationConverter.toResponse(allocation);
    }

    private BoothAllocation getEntity(Long allocationId) {
        return boothAllocationRepository
                .findById(allocationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND));
    }
}
