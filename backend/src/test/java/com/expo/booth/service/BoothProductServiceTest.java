package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private BoothProductRepository boothProductRepository;
    private BoothRepository boothRepository;
    private RecruitmentNoticeRepository recruitmentNoticeRepository;
    private BoothProductService service;

    @BeforeEach
    void setUp() {
        boothProductRepository = mock(BoothProductRepository.class);
        boothRepository = mock(BoothRepository.class);
        recruitmentNoticeRepository = mock(RecruitmentNoticeRepository.class);
        service =
                new BoothProductService(
                        boothProductRepository,
                        boothRepository,
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
        LocalDateTime start = LocalDateTime.of(2026, 9, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 1, 0, 0);
        CreateBoothProductRequest request =
                new CreateBoothProductRequest(
                        NOTICE_ID, BOOTH_ID, BigDecimal.TEN, null, true, null, start, end, true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_SALES_PERIOD_INVALID);
        verify(recruitmentNoticeRepository, never()).existsById(any());
    }

    @Test
    void createRejectsWhenNoticeNotFound() {
        when(recruitmentNoticeRepository.existsById(NOTICE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_FOUND);
    }

    @Test
    void createRejectsWhenBoothNotFound() {
        when(recruitmentNoticeRepository.existsById(NOTICE_ID)).thenReturn(true);
        when(boothRepository.existsById(BOOTH_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_NOT_FOUND);
    }

    @Test
    void createRejectsDuplicateBoothProduct() {
        when(recruitmentNoticeRepository.existsById(NOTICE_ID)).thenReturn(true);
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
    void createSucceedsAndComputesTotalPrice() {
        when(recruitmentNoticeRepository.existsById(NOTICE_ID)).thenReturn(true);
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
        when(recruitmentNoticeRepository.existsById(NOTICE_ID)).thenReturn(true);
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
        when(recruitmentNoticeRepository.existsById(NOTICE_ID)).thenReturn(true);
        when(boothRepository.existsById(BOOTH_ID)).thenReturn(true);
        when(boothProductRepository.existsByRecruitmentNoticeIdAndBoothId(NOTICE_ID, BOOTH_ID))
                .thenReturn(false);
        when(boothProductRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("some other constraint"));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(DataIntegrityViolationException.class);
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
                                LocalDateTime.now().plusDays(1),
                                LocalDateTime.now().plusDays(2),
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
                                LocalDateTime.now().minusDays(2),
                                LocalDateTime.now().minusDays(1),
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
