package com.expo.booth.service;

import com.expo.booth.converter.BoothProductConverter;
import com.expo.booth.dto.BoothProductResponse;
import com.expo.booth.dto.CreateBoothProductRequest;
import com.expo.booth.entity.BoothProduct;
import com.expo.booth.entity.BoothSalesStatus;
import com.expo.booth.repository.BoothProductRepository;
import com.expo.booth.repository.BoothRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 특정 모집공고에서 판매되는 부스 상품 등록·조회·판매 상태 관리. */
@Slf4j
@Service
public class BoothProductService {

    private static final Set<BoothSalesStatus> ADMIN_EDITABLE_STATUSES =
            Set.of(
                    BoothSalesStatus.AVAILABLE,
                    BoothSalesStatus.UNAVAILABLE,
                    BoothSalesStatus.CANCELED);

    private final BoothProductRepository boothProductRepository;
    private final BoothRepository boothRepository;
    private final RecruitmentNoticeRepository recruitmentNoticeRepository;
    private final BoothProductConverter boothProductConverter;

    public BoothProductService(
            BoothProductRepository boothProductRepository,
            BoothRepository boothRepository,
            RecruitmentNoticeRepository recruitmentNoticeRepository,
            BoothProductConverter boothProductConverter) {
        this.boothProductRepository = boothProductRepository;
        this.boothRepository = boothRepository;
        this.recruitmentNoticeRepository = recruitmentNoticeRepository;
        this.boothProductConverter = boothProductConverter;
    }

    /** 부스 상품 등록. 같은 공고 안에서 같은 부스에 상품을 두 번 등록할 수 없다. */
    @Transactional
    public BoothProductResponse create(CreateBoothProductRequest request) {
        if (request.salesStartAt() != null
                && request.salesEndAt() != null
                && !request.salesEndAt().isAfter(request.salesStartAt())) {
            throw new BusinessException(ErrorCode.BOOTH_SALES_PERIOD_INVALID);
        }
        if (!recruitmentNoticeRepository.existsById(request.recruitmentNoticeId())) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_FOUND);
        }
        if (!boothRepository.existsById(request.boothId())) {
            throw new BusinessException(ErrorCode.BOOTH_NOT_FOUND);
        }
        if (boothProductRepository.existsByRecruitmentNoticeIdAndBoothId(
                request.recruitmentNoticeId(), request.boothId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_BOOTH_PRODUCT);
        }
        if (boothProductRepository.existsByBoothIdAndRecruitmentNoticeIdNotAndSalesStatusNot(
                request.boothId(), request.recruitmentNoticeId(), BoothSalesStatus.CANCELED)) {
            throw new BusinessException(ErrorCode.BOOTH_IN_USE_BY_OTHER_NOTICE);
        }
        BoothProduct product =
                BoothProduct.create(
                                request.recruitmentNoticeId(),
                                request.boothId(),
                                request.supplyPrice(),
                                request.vatAmount(),
                                request.vatIncluded(),
                                request.includedItems())
                        .schedule(
                                request.salesStartAt(),
                                request.salesEndAt(),
                                request.paymentEnabled());
        try {
            BoothProduct saved = boothProductRepository.saveAndFlush(product);
            return boothProductConverter.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("uq_booth_products_booth")) {
                throw new BusinessException(ErrorCode.DUPLICATE_BOOTH_PRODUCT);
            }
            log.warn(
                    "부스 상품 저장 중 예상하지 못한 무결성 제약 위반. recruitmentNoticeId={}, boothId={}",
                    request.recruitmentNoticeId(),
                    request.boothId(),
                    e);
            throw e;
        }
    }

    /** 공고별 부스 상품 목록 조회 (관리자, 전체 상태). */
    @Transactional(readOnly = true)
    public List<BoothProductResponse> listForAdmin(Long recruitmentNoticeId) {
        return boothProductRepository.findAllByRecruitmentNoticeId(recruitmentNoticeId).stream()
                .map(boothProductConverter::toResponse)
                .toList();
    }

    /** 공고별 구매 가능한 부스 상품 목록 조회 (공개). 결제 가능하고 판매 기간 안에 있는 상품만 반환한다. */
    @Transactional(readOnly = true)
    public List<BoothProductResponse> listAvailable(Long recruitmentNoticeId) {
        Instant now = Instant.now();
        return boothProductRepository
                .findAllByRecruitmentNoticeIdAndSalesStatus(
                        recruitmentNoticeId, BoothSalesStatus.AVAILABLE)
                .stream()
                .filter(BoothProduct::isPaymentEnabled)
                .filter(
                        product ->
                                (product.getSalesStartAt() == null
                                                || !now.isBefore(product.getSalesStartAt()))
                                        && (product.getSalesEndAt() == null
                                                || !now.isAfter(product.getSalesEndAt())))
                .map(boothProductConverter::toResponse)
                .toList();
    }

    /** 부스 상품 상세 조회. */
    @Transactional(readOnly = true)
    public BoothProductResponse get(Long productId) {
        return boothProductConverter.toResponse(getEntity(productId));
    }

    /**
     * 관리자 판매 상태 변경. AVAILABLE·UNAVAILABLE·CANCELED 사이에서만 바꿀 수 있고, 이미 주문 흐름이 점유한
     * RESERVED·SOLD 상태는 이 경로로 바꿀 수 없다.
     */
    @Transactional
    public BoothProductResponse updateSalesStatus(Long productId, BoothSalesStatus salesStatus) {
        BoothProduct product = getEntity(productId);
        if (!ADMIN_EDITABLE_STATUSES.contains(product.getSalesStatus())
                || !ADMIN_EDITABLE_STATUSES.contains(salesStatus)) {
            throw new BusinessException(ErrorCode.BOOTH_PRODUCT_NOT_EDITABLE);
        }
        product.changeSalesStatus(salesStatus);
        return boothProductConverter.toResponse(product);
    }

    private BoothProduct getEntity(Long productId) {
        return boothProductRepository
                .findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_PRODUCT_NOT_FOUND));
    }
}
