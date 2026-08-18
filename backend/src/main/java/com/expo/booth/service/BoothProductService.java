package com.expo.booth.service;

import com.expo.booth.converter.BoothProductConverter;
import com.expo.booth.dto.BoothProductResponse;
import com.expo.booth.dto.CreateBoothProductRequest;
import com.expo.booth.entity.Booth;
import com.expo.booth.entity.BoothProduct;
import com.expo.booth.entity.BoothSalesStatus;
import com.expo.booth.repository.BoothProductRepository;
import com.expo.booth.repository.BoothRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.recruitment.entity.RecruitmentNoticeStatus;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import com.expo.venue.entity.VenueHall;
import com.expo.venue.entity.VenueZone;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VenueZoneRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final VenueZoneRepository venueZoneRepository;
    private final VenueHallRepository venueHallRepository;
    private final RecruitmentNoticeRepository recruitmentNoticeRepository;
    private final BoothProductConverter boothProductConverter;

    public BoothProductService(
            BoothProductRepository boothProductRepository,
            BoothRepository boothRepository,
            VenueZoneRepository venueZoneRepository,
            VenueHallRepository venueHallRepository,
            RecruitmentNoticeRepository recruitmentNoticeRepository,
            BoothProductConverter boothProductConverter) {
        this.boothProductRepository = boothProductRepository;
        this.boothRepository = boothRepository;
        this.venueZoneRepository = venueZoneRepository;
        this.venueHallRepository = venueHallRepository;
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
        List<Long> otherNoticeIds =
                boothProductRepository.findOtherRecruitmentNoticeIdsUsingBooth(
                        request.boothId(),
                        request.recruitmentNoticeId(),
                        BoothSalesStatus.CANCELED);
        if (!otherNoticeIds.isEmpty()
                && recruitmentNoticeRepository.existsByIdInAndStatusNot(
                        otherNoticeIds, RecruitmentNoticeStatus.CANCELED)) {
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
            return toResponseWithLocation(saved);
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
        return toResponsesWithLocation(
                boothProductRepository.findAllByRecruitmentNoticeId(recruitmentNoticeId));
    }

    /** 공고별 구매 가능한 부스 상품 목록 조회 (공개). 결제 가능하고 판매 기간 안에 있는 상품만 반환한다. */
    @Transactional(readOnly = true)
    public List<BoothProductResponse> listAvailable(Long recruitmentNoticeId) {
        Instant now = Instant.now();
        List<BoothProduct> products =
                boothProductRepository
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
                        .toList();
        return toResponsesWithLocation(products);
    }

    /** 부스 상품 상세 조회. */
    @Transactional(readOnly = true)
    public BoothProductResponse get(Long productId) {
        return toResponseWithLocation(getEntity(productId));
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
        return toResponseWithLocation(product);
    }

    private BoothProduct getEntity(Long productId) {
        return boothProductRepository
                .findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_PRODUCT_NOT_FOUND));
    }

    private BoothProductResponse toResponseWithLocation(BoothProduct product) {
        return toResponsesWithLocation(List.of(product)).get(0);
    }

    /**
     * 부스 상품이 몇 홀·어느 구역의 어떤 부스인지 배치 조회로 채워서 응답을 만든다. N+1 을 피하기 위해 부스 → 구역 → 홀
     * 순서로 각각 한 번씩만 조회한다.
     */
    private List<BoothProductResponse> toResponsesWithLocation(List<BoothProduct> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Long, Booth> boothsById =
                boothRepository
                        .findAllById(products.stream().map(BoothProduct::getBoothId).toList())
                        .stream()
                        .collect(Collectors.toMap(Booth::getId, Function.identity()));
        Map<Long, VenueZone> zonesById =
                venueZoneRepository
                        .findAllById(
                                boothsById.values().stream().map(Booth::getVenueZoneId).toList())
                        .stream()
                        .collect(Collectors.toMap(VenueZone::getId, Function.identity()));
        Map<Long, VenueHall> hallsById =
                venueHallRepository
                        .findAllById(zonesById.values().stream().map(VenueZone::getHallId).toList())
                        .stream()
                        .collect(Collectors.toMap(VenueHall::getId, Function.identity()));
        return products.stream()
                .map(
                        product -> {
                            Booth booth = boothsById.get(product.getBoothId());
                            VenueZone zone =
                                    booth != null ? zonesById.get(booth.getVenueZoneId()) : null;
                            VenueHall hall = zone != null ? hallsById.get(zone.getHallId()) : null;
                            return boothProductConverter.toResponse(product, booth, zone, hall);
                        })
                .toList();
    }
}
