package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.expo.recruitment.entity.RecruitmentNotice;
import com.expo.recruitment.entity.RecruitmentNoticeStatus;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import com.expo.venue.entity.VenueHall;
import com.expo.venue.entity.VenueZone;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VenueZoneRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** {@link BoothProductService} 의 등록 검증과 판매 상태 변경 규칙을 확인한다. */
class BoothProductServiceTest {

    private static final Long NOTICE_ID = 1L;
    private static final Long BOOTH_ID = 2L;
    private static final Long PRODUCT_ID = 3L;
    private static final Long OTHER_NOTICE_ID = 4L;

    private BoothProductRepository boothProductRepository;
    private BoothRepository boothRepository;
    private VenueZoneRepository venueZoneRepository;
    private VenueHallRepository venueHallRepository;
    private RecruitmentNoticeRepository recruitmentNoticeRepository;
    private BoothProductService service;

    @BeforeEach
    void setUp() {
        boothProductRepository = mock(BoothProductRepository.class);
        boothRepository = mock(BoothRepository.class);
        venueZoneRepository = mock(VenueZoneRepository.class);
        venueHallRepository = mock(VenueHallRepository.class);
        recruitmentNoticeRepository = mock(RecruitmentNoticeRepository.class);
        service =
                new BoothProductService(
                        boothProductRepository,
                        boothRepository,
                        venueZoneRepository,
                        venueHallRepository,
                        recruitmentNoticeRepository,
                        new BoothProductConverter());
    }

    private CreateBoothProductRequest request() {
        return new CreateBoothProductRequest(
                NOTICE_ID,
                BOOTH_ID,
                BigDecimal.valueOf(1_000_000),
                BigDecimal.valueOf(100_000),
                true,
                null,
                null,
                null,
                true);
    }

    private RecruitmentNotice noticeWithStatus(RecruitmentNoticeStatus status) {
        try {
            java.lang.reflect.Constructor<RecruitmentNotice> constructor =
                    RecruitmentNotice.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            RecruitmentNotice notice = constructor.newInstance();
            java.lang.reflect.Field statusField =
                    RecruitmentNotice.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(notice, status);
            return notice;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private BoothProduct product() {
        return BoothProduct.create(
                        NOTICE_ID,
                        BOOTH_ID,
                        BigDecimal.valueOf(1_000_000),
                        BigDecimal.valueOf(100_000),
                        true,
                        null)
                .schedule(null, null, true);
    }

    @Test
    void createRejectsWhenSalesPeriodInvalid() {
        Instant start = Instant.parse("2026-09-10T00:00:00Z");
        Instant end = Instant.parse("2026-09-01T00:00:00Z");
        CreateBoothProductRequest request =
                new CreateBoothProductRequest(
                        NOTICE_ID, BOOTH_ID, BigDecimal.TEN, null, true, null, start, end, true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_SALES_PERIOD_INVALID);
        verify(recruitmentNoticeRepository, never()).findById(any());
    }

    @Test
    void createRejectsWhenNoticeNotFound() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_FOUND);
    }

    @Test
    void createRejectsWhenNoticeAlreadyClosed() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.CLOSED)));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_PRODUCT_CREATION_NOT_ALLOWED);
        verify(boothRepository, never()).existsById(any());
    }

    @Test
    void createRejectsWhenBoothNotFound() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.OPEN)));
        when(boothRepository.existsById(BOOTH_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_NOT_FOUND);
    }

    @Test
    void createRejectsDuplicateBoothProduct() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.OPEN)));
        when(boothRepository.existsById(BOOTH_ID)).thenReturn(true);
        when(boothProductRepository.existsByRecruitmentNoticeIdAndBoothId(NOTICE_ID, BOOTH_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_BOOTH_PRODUCT);
        verify(boothProductRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsWhenBoothInUseByOtherActiveNotice() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.OPEN)));
        when(boothRepository.existsById(BOOTH_ID)).thenReturn(true);
        when(boothProductRepository.existsByRecruitmentNoticeIdAndBoothId(NOTICE_ID, BOOTH_ID))
                .thenReturn(false);
        when(boothProductRepository.findOtherRecruitmentNoticeIdsUsingBooth(
                        BOOTH_ID, NOTICE_ID, BoothSalesStatus.CANCELED))
                .thenReturn(List.of(OTHER_NOTICE_ID));
        when(recruitmentNoticeRepository.existsByIdInAndStatusNot(
                        List.of(OTHER_NOTICE_ID), RecruitmentNoticeStatus.CANCELED))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_IN_USE_BY_OTHER_NOTICE);
        verify(boothProductRepository, never()).saveAndFlush(any());
    }

    /** 다른 공고가 이미 취소됐다면, 그 공고의 부스 상품이 취소되지 않은 채 남아있어도 재사용을 막지 않는다. */
    @Test
    void createAllowsBoothWhenOtherNoticeIsCanceled() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.OPEN)));
        when(boothRepository.existsById(BOOTH_ID)).thenReturn(true);
        when(boothProductRepository.existsByRecruitmentNoticeIdAndBoothId(NOTICE_ID, BOOTH_ID))
                .thenReturn(false);
        when(boothProductRepository.findOtherRecruitmentNoticeIdsUsingBooth(
                        BOOTH_ID, NOTICE_ID, BoothSalesStatus.CANCELED))
                .thenReturn(List.of(OTHER_NOTICE_ID));
        when(recruitmentNoticeRepository.existsByIdInAndStatusNot(
                        List.of(OTHER_NOTICE_ID), RecruitmentNoticeStatus.CANCELED))
                .thenReturn(false);
        when(boothProductRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BoothProductResponse response = service.create(request());

        assertThat(response.recruitmentNoticeId()).isEqualTo(NOTICE_ID);
    }

    /** 응답에 부스·구역·홀 위치 정보가 배치 조회 결과에서 정확히 매핑돼야 한다. */
    @Test
    void createReturnsLocationInfoFromBoothZoneHall() throws ReflectiveOperationException {
        Long zoneId = 500L;
        Long hallId = 700L;
        Booth booth =
                withId(
                        Booth.create(
                                zoneId,
                                null,
                                "A-01",
                                "SQUARE",
                                BigDecimal.TEN,
                                null,
                                BigDecimal.TEN,
                                "M"),
                        BOOTH_ID);
        VenueZone zone =
                withId(VenueZone.create(hallId, "ZONE-1", "1구역", 10, null, null, null), zoneId);
        VenueHall hall = withId(VenueHall.create(1L, "HALL-A", "A홀", null, null, null), hallId);

        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.OPEN)));
        when(boothRepository.existsById(BOOTH_ID)).thenReturn(true);
        when(boothProductRepository.existsByRecruitmentNoticeIdAndBoothId(NOTICE_ID, BOOTH_ID))
                .thenReturn(false);
        when(boothProductRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(boothRepository.findAllById(any())).thenReturn(List.of(booth));
        when(venueZoneRepository.findAllById(any())).thenReturn(List.of(zone));
        when(venueHallRepository.findAllById(any())).thenReturn(List.of(hall));

        BoothProductResponse response = service.create(request());

        assertThat(response.boothNumber()).isEqualTo("A-01");
        assertThat(response.venueZoneId()).isEqualTo(zoneId);
        assertThat(response.venueZoneName()).isEqualTo("1구역");
        assertThat(response.venueHallId()).isEqualTo(hallId);
        assertThat(response.venueHallName()).isEqualTo("A홀");
    }

    /** id 는 {@code @GeneratedValue} 라 팩토리로 못 채워서, 조회된 것처럼 리플렉션으로 세팅한다. */
    private static <T> T withId(T entity, Long id) throws ReflectiveOperationException {
        java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
        return entity;
    }

    @Test
    void createSucceedsAndComputesTotalPrice() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.OPEN)));
        when(boothRepository.existsById(BOOTH_ID)).thenReturn(true);
        when(boothProductRepository.existsByRecruitmentNoticeIdAndBoothId(NOTICE_ID, BOOTH_ID))
                .thenReturn(false);
        when(boothProductRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BoothProductResponse response = service.create(request());

        assertThat(response.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(1_100_000));
        assertThat(response.salesStatus()).isEqualTo(BoothSalesStatus.AVAILABLE);
    }

    /** 사전 중복 검사를 통과해도 동시 삽입으로 제약 위반이 나면 같은 오류로 변환돼야 한다. */
    @Test
    void createTranslatesConstraintViolationToDuplicate() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.OPEN)));
        when(boothRepository.existsById(BOOTH_ID)).thenReturn(true);
        when(boothProductRepository.existsByRecruitmentNoticeIdAndBoothId(NOTICE_ID, BOOTH_ID))
                .thenReturn(false);
        when(boothProductRepository.saveAndFlush(any()))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "duplicate key value violates unique constraint"
                                        + " \"uq_booth_products_booth\""));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_BOOTH_PRODUCT);
    }

    @Test
    void createRethrowsUnrelatedConstraintViolation() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.OPEN)));
        when(boothRepository.existsById(BOOTH_ID)).thenReturn(true);
        when(boothProductRepository.existsByRecruitmentNoticeIdAndBoothId(NOTICE_ID, BOOTH_ID))
                .thenReturn(false);
        when(boothProductRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("some other constraint"));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private CreateBoothProductRequest requestForBooth(Long boothId) {
        return new CreateBoothProductRequest(
                NOTICE_ID,
                boothId,
                BigDecimal.valueOf(1_000_000),
                BigDecimal.valueOf(100_000),
                true,
                null,
                null,
                null,
                true);
    }

    @Test
    void createBulkRejectsDuplicateNoticeBoothPairWithinBatch() {
        assertThatThrownBy(
                        () ->
                                service.createBulk(
                                        List.of(
                                                requestForBooth(BOOTH_ID),
                                                requestForBooth(BOOTH_ID))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_BOOTH_PRODUCT);
        verify(boothProductRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void createBulkRejectsWhenAnyItemFailsValidation() {
        Long otherBoothId = 99L;
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.OPEN)));
        when(boothRepository.existsById(BOOTH_ID)).thenReturn(true);
        when(boothProductRepository.existsByRecruitmentNoticeIdAndBoothId(NOTICE_ID, BOOTH_ID))
                .thenReturn(false);
        when(boothRepository.existsById(otherBoothId)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.createBulk(
                                        List.of(
                                                requestForBooth(BOOTH_ID),
                                                requestForBooth(otherBoothId))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_NOT_FOUND);
        verify(boothProductRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void createBulkSucceeds() {
        Long otherBoothId = 99L;
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.OPEN)));
        when(boothRepository.existsById(BOOTH_ID)).thenReturn(true);
        when(boothRepository.existsById(otherBoothId)).thenReturn(true);
        when(boothProductRepository.existsByRecruitmentNoticeIdAndBoothId(eq(NOTICE_ID), any()))
                .thenReturn(false);
        when(boothProductRepository.saveAllAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<BoothProductResponse> responses =
                service.createBulk(
                        List.of(requestForBooth(BOOTH_ID), requestForBooth(otherBoothId)));

        assertThat(responses).hasSize(2);
    }

    @Test
    void listAvailableFiltersByAvailableStatus() {
        when(boothProductRepository.findAllByRecruitmentNoticeIdAndSalesStatus(
                        NOTICE_ID, BoothSalesStatus.AVAILABLE))
                .thenReturn(List.of(product()));

        List<BoothProductResponse> responses = service.listAvailable(NOTICE_ID);

        assertThat(responses).hasSize(1);
    }

    /** AVAILABLE 상태여도 결제 불가로 설정된 상품은 공개 목록에서 빠져야 한다. */
    @Test
    void listAvailableExcludesPaymentDisabledProduct() {
        BoothProduct paymentDisabled =
                BoothProduct.create(
                                NOTICE_ID,
                                BOOTH_ID,
                                BigDecimal.valueOf(1_000_000),
                                BigDecimal.valueOf(100_000),
                                true,
                                null)
                        .schedule(null, null, false);
        when(boothProductRepository.findAllByRecruitmentNoticeIdAndSalesStatus(
                        NOTICE_ID, BoothSalesStatus.AVAILABLE))
                .thenReturn(List.of(paymentDisabled));

        List<BoothProductResponse> responses = service.listAvailable(NOTICE_ID);

        assertThat(responses).isEmpty();
    }

    /** 판매 기간 밖(시작 전·종료 후)인 상품은 공개 목록에서 빠져야 한다. */
    @Test
    void listAvailableExcludesProductOutsideSalesWindow() {
        BoothProduct notYetOnSale =
                BoothProduct.create(
                                NOTICE_ID,
                                BOOTH_ID,
                                BigDecimal.valueOf(1_000_000),
                                BigDecimal.valueOf(100_000),
                                true,
                                null)
                        .schedule(
                                Instant.now().plus(Duration.ofDays(1)),
                                Instant.now().plus(Duration.ofDays(2)),
                                true);
        BoothProduct alreadyEnded =
                BoothProduct.create(
                                NOTICE_ID,
                                BOOTH_ID,
                                BigDecimal.valueOf(1_000_000),
                                BigDecimal.valueOf(100_000),
                                true,
                                null)
                        .schedule(
                                Instant.now().minus(Duration.ofDays(2)),
                                Instant.now().minus(Duration.ofDays(1)),
                                true);
        when(boothProductRepository.findAllByRecruitmentNoticeIdAndSalesStatus(
                        NOTICE_ID, BoothSalesStatus.AVAILABLE))
                .thenReturn(List.of(notYetOnSale, alreadyEnded));

        List<BoothProductResponse> responses = service.listAvailable(NOTICE_ID);

        assertThat(responses).isEmpty();
    }

    @Test
    void getRejectsWhenNotFound() {
        when(boothProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_PRODUCT_NOT_FOUND);
    }

    @Test
    void updateSalesStatusSucceedsBetweenEditableStatuses() {
        BoothProduct product = product();
        when(boothProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        BoothProductResponse response =
                service.updateSalesStatus(PRODUCT_ID, BoothSalesStatus.UNAVAILABLE);

        assertThat(response.salesStatus()).isEqualTo(BoothSalesStatus.UNAVAILABLE);
    }

    /** 이미 예약·판매된 상품은 관리자가 이 경로로 상태를 바꿀 수 없어야 한다. */
    @Test
    void updateSalesStatusRejectsWhenCurrentStatusIsReserved() {
        BoothProduct product = product();
        product.changeSalesStatus(BoothSalesStatus.RESERVED);
        when(boothProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.updateSalesStatus(PRODUCT_ID, BoothSalesStatus.AVAILABLE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_PRODUCT_NOT_EDITABLE);
    }

    /** SOLD·RESERVED 로의 전이는 주문 흐름 전용이라 관리자가 직접 지정할 수 없어야 한다. */
    @Test
    void updateSalesStatusRejectsWhenTargetStatusIsSold() {
        BoothProduct product = product();
        when(boothProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.updateSalesStatus(PRODUCT_ID, BoothSalesStatus.SOLD))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_PRODUCT_NOT_EDITABLE);
    }
}
