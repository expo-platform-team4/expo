package com.expo.booth.service;

import com.expo.booth.converter.BoothAllocationConverter;
import com.expo.booth.dto.BoothAllocationResponse;
import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.entity.BoothAllocationStatus;
import com.expo.booth.entity.BoothProduct;
import com.expo.booth.entity.BoothSalesStatus;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothProductRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 부스 확정 배정 조회·취소. 배정 생성 자체는 결제 승인 흐름({@link BoothPaymentService})이 담당한다. */
@Service
public class BoothAllocationService {

    private final BoothAllocationRepository boothAllocationRepository;
    private final BoothProductRepository boothProductRepository;
    private final BoothAllocationConverter boothAllocationConverter;

    public BoothAllocationService(
            BoothAllocationRepository boothAllocationRepository,
            BoothProductRepository boothProductRepository,
            BoothAllocationConverter boothAllocationConverter) {
        this.boothAllocationRepository = boothAllocationRepository;
        this.boothProductRepository = boothProductRepository;
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

    /** 관리자용 배정 목록 조회. */
    @Transactional(readOnly = true)
    public List<BoothAllocationResponse> listForAdmin() {
        return boothAllocationRepository.findAll().stream()
                .map(boothAllocationConverter::toResponse)
                .toList();
    }

    /** 관리자용 배정 상세 조회. */
    @Transactional(readOnly = true)
    public BoothAllocationResponse getForAdmin(Long allocationId) {
        return boothAllocationConverter.toResponse(getEntity(allocationId));
    }

    /** 관리자 배정 취소. 부스 이중 배정 등 운영상 정정이 필요할 때만 쓰며, 부스 상품을 다시 판매 가능하게 되돌린다. */
    @Transactional
    public BoothAllocationResponse cancel(Long allocationId, String reason) {
        BoothAllocation allocation = getEntity(allocationId);
        if (allocation.getStatus() != BoothAllocationStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_CANCELABLE);
        }
        allocation.cancel(reason);
        boothProductRepository
                .findById(allocation.getBoothProductId())
                .ifPresent(this::revertToAvailable);
        return boothAllocationConverter.toResponse(allocation);
    }

    private void revertToAvailable(BoothProduct product) {
        product.changeSalesStatus(BoothSalesStatus.AVAILABLE);
    }

    private BoothAllocation getEntity(Long allocationId) {
        return boothAllocationRepository
                .findById(allocationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND));
    }
}
