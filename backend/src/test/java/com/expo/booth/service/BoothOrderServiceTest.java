package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.booth.converter.BoothOrderConverter;
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
import com.expo.participation.repository.ParticipationApplicationRepository;
import com.expo.recruitment.entity.RecruitmentNotice;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link BoothOrderService} 의 주문 생성·취소 규칙을 확인한다. */
class BoothOrderServiceTest {

    private static final Long APPLICATION_ID = 1L;
    private static final Long CLIENT_USER_ID = 2L;
    private static final Long BOOTH_PRODUCT_ID = 3L;
    private static final Long ORDER_ID = 4L;
    private static final Long NOTICE_ID = 5L;
    private static final Long ADMIN_ID = 6L;

    private BoothOrderRepository boothOrderRepository;
    private BoothReservationRepository boothReservationRepository;
    private BoothProductRepository boothProductRepository;
    private ParticipationApplicationRepository participationApplicationRepository;
    private RecruitmentNoticeRepository recruitmentNoticeRepository;
    private BoothOrderService service;

    @BeforeEach
    void setUp() {
        boothOrderRepository = mock(BoothOrderRepository.class);
        boothReservationRepository = mock(BoothReservationRepository.class);
        boothProductRepository = mock(BoothProductRepository.class);
        participationApplicationRepository = mock(ParticipationApplicationRepository.class);
        recruitmentNoticeRepository = mock(RecruitmentNoticeRepository.class);
        service =
                new BoothOrderService(
                        boothOrderRepository,
                        boothReservationRepository,
                        boothProductRepository,
                        participationApplicationRepository,
                        recruitmentNoticeRepository,
                        new BoothOrderConverter());
        when(recruitmentNoticeRepository.findById(NOTICE_ID)).thenReturn(Optional.of(openNotice()));
    }

    private RecruitmentNotice openNotice() {
        RecruitmentNotice notice =
                RecruitmentNotice.create(
                        100L,
                        200L,
                        "제목",
                        "내용",
                        Instant.now(),
                        Instant.now().plus(Duration.ofDays(1)),
                        ADMIN_ID);
        notice.publish();
        return notice;
    }

    private ParticipationApplication draftApplication(Long boothProductId) {
        return ParticipationApplication.create(
                NOTICE_ID, CLIENT_USER_ID, "테스트 참가기업", null, null, boothProductId);
    }

    private BoothProduct availableProduct() {
        return BoothProduct.create(
                        NOTICE_ID,
                        10L,
                        BigDecimal.valueOf(1_000_000),
                        BigDecimal.valueOf(100_000),
                        true,
                        null)
                .schedule(null, null, true);
    }

    @Test
    void createRejectsWhenApplicationNotFound() {
        when(participationApplicationRepository.findByIdAndClientUserId(
                        APPLICATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(APPLICATION_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARTICIPATION_APPLICATION_NOT_FOUND);
    }

    @Test
    void createRejectsWhenApplicationNotDraft() {
        ParticipationApplication application = draftApplication(BOOTH_PRODUCT_ID);
        application.startPayment(99L);
        when(participationApplicationRepository.findByIdAndClientUserId(
                        APPLICATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.create(APPLICATION_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ORDER_NOT_ALLOWED);
    }

    /**
     * 신청서를 만든 뒤 공고가 조기 마감·취소돼도 신청서 자체는 계속 DRAFT 로 남아있을 수 있다. 신청서 상태만 보면
     * 주문·결제를 계속 진행할 수 있어 보이므로, 공고 상태를 다시 확인해서 막아야 한다.
     */
    @Test
    void createRejectsWhenNoticeNotOpen() {
        RecruitmentNotice closedNotice = openNotice();
        closedNotice.close();
        when(recruitmentNoticeRepository.findById(NOTICE_ID)).thenReturn(Optional.of(closedNotice));
        when(participationApplicationRepository.findByIdAndClientUserId(
                        APPLICATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(draftApplication(BOOTH_PRODUCT_ID)));

        assertThatThrownBy(() -> service.create(APPLICATION_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_OPEN);
        verify(boothProductRepository, never()).findById(any());
    }

    @Test
    void createRejectsWhenNoBoothProductSelected() {
        when(participationApplicationRepository.findByIdAndClientUserId(
                        APPLICATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(draftApplication(null)));

        assertThatThrownBy(() -> service.create(APPLICATION_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_PRODUCT_NOT_SELECTED);
    }

    @Test
    void createRejectsWhenBoothProductNotFound() {
        when(participationApplicationRepository.findByIdAndClientUserId(
                        APPLICATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(draftApplication(BOOTH_PRODUCT_ID)));
        when(boothProductRepository.findById(BOOTH_PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(APPLICATION_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_PRODUCT_NOT_FOUND);
    }

    @Test
    void createRejectsWhenBoothProductNotAvailable() {
        BoothProduct product = availableProduct();
        product.reserve();
        when(participationApplicationRepository.findByIdAndClientUserId(
                        APPLICATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(draftApplication(BOOTH_PRODUCT_ID)));
        when(boothProductRepository.findById(BOOTH_PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.create(APPLICATION_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_PRODUCT_NOT_AVAILABLE);
    }

    @Test
    void createRejectsWhenSalesPeriodNotOpen() {
        BoothProduct product =
                BoothProduct.create(
                                NOTICE_ID,
                                10L,
                                BigDecimal.valueOf(1_000_000),
                                BigDecimal.valueOf(100_000),
                                true,
                                null)
                        .schedule(
                                Instant.now().plus(Duration.ofDays(1)),
                                Instant.now().plus(Duration.ofDays(2)),
                                true);
        when(participationApplicationRepository.findByIdAndClientUserId(
                        APPLICATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(draftApplication(BOOTH_PRODUCT_ID)));
        when(boothProductRepository.findById(BOOTH_PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.create(APPLICATION_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_PRODUCT_SALES_NOT_OPEN);
    }

    @Test
    void createSucceedsAndReservesProduct() {
        BoothProduct product = availableProduct();
        ParticipationApplication application = draftApplication(BOOTH_PRODUCT_ID);
        when(participationApplicationRepository.findByIdAndClientUserId(
                        APPLICATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(application));
        when(boothProductRepository.findById(BOOTH_PRODUCT_ID)).thenReturn(Optional.of(product));
        when(boothOrderRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(boothReservationRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(APPLICATION_ID, CLIENT_USER_ID);

        assertThat(response.status()).isEqualTo(BoothOrderStatus.PENDING_PAYMENT);
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_100_000));
        assertThat(product.getSalesStatus()).isEqualTo(BoothSalesStatus.RESERVED);
        assertThat(application.getStatus().name()).isEqualTo("PAYMENT_PENDING");
        verify(boothReservationRepository).saveAndFlush(any());
    }

    @Test
    void getMineRejectsWhenNotFound() {
        when(boothOrderRepository.findByIdAndClientUserId(ORDER_ID, CLIENT_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMine(ORDER_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ORDER_NOT_FOUND);
    }

    private BoothOrder pendingOrder() {
        return BoothOrder.create(
                APPLICATION_ID,
                CLIENT_USER_ID,
                BOOTH_PRODUCT_ID,
                "BO12345",
                BigDecimal.valueOf(1_100_000),
                "idem-key",
                Instant.now().plus(Duration.ofMinutes(15)));
    }

    @Test
    void cancelRejectsWhenNotFound() {
        when(boothOrderRepository.findByIdAndClientUserId(ORDER_ID, CLIENT_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(ORDER_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ORDER_NOT_FOUND);
    }

    @Test
    void cancelRejectsWhenNotPendingPayment() {
        BoothOrder order = pendingOrder();
        order.cancel();
        when(boothOrderRepository.findByIdAndClientUserId(ORDER_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancel(ORDER_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ORDER_NOT_CANCELABLE);
    }

    @Test
    void cancelReleasesReservationAndRevertsProductAndApplication() {
        BoothOrder order = pendingOrder();
        BoothProduct product = availableProduct();
        product.reserve();
        ParticipationApplication application = draftApplication(BOOTH_PRODUCT_ID);
        application.startPayment(ORDER_ID);
        BoothReservation reservation =
                BoothReservation.create(BOOTH_PRODUCT_ID, ORDER_ID, CLIENT_USER_ID, Instant.now());

        when(boothOrderRepository.findByIdAndClientUserId(ORDER_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(order));
        when(boothReservationRepository.findFirstByBoothOrderIdAndStatus(
                        ORDER_ID, BoothReservationStatus.ACTIVE))
                .thenReturn(Optional.of(reservation));
        when(boothProductRepository.findById(BOOTH_PRODUCT_ID)).thenReturn(Optional.of(product));
        when(participationApplicationRepository.findByIdAndClientUserId(
                        APPLICATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(application));

        var response = service.cancel(ORDER_ID, CLIENT_USER_ID);

        assertThat(response.status()).isEqualTo(BoothOrderStatus.CANCELED);
        assertThat(reservation.getStatus()).isEqualTo(BoothReservationStatus.RELEASED);
        assertThat(product.getSalesStatus()).isEqualTo(BoothSalesStatus.AVAILABLE);
        assertThat(application.getStatus().name()).isEqualTo("DRAFT");
    }
}
