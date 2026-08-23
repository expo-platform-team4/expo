package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.booth.converter.BoothPaymentConverter;
import com.expo.booth.dto.BoothPaymentResponse;
import com.expo.booth.dto.InitiateBoothPaymentResponse;
import com.expo.booth.entity.BoothOrder;
import com.expo.booth.entity.BoothPayment;
import com.expo.booth.entity.BoothPaymentStatus;
import com.expo.booth.entity.BoothProduct;
import com.expo.booth.entity.BoothReservation;
import com.expo.booth.entity.BoothReservationStatus;
import com.expo.booth.entity.BoothSalesStatus;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothOrderRepository;
import com.expo.booth.repository.BoothPaymentHistoryRepository;
import com.expo.booth.repository.BoothPaymentRepository;
import com.expo.booth.repository.BoothProductRepository;
import com.expo.booth.repository.BoothReservationRepository;
import com.expo.common.config.TossApiException;
import com.expo.common.config.TossConfirmResult;
import com.expo.common.config.TossPaymentClient;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.participation.entity.ParticipationApplication;
import com.expo.participation.repository.ParticipationApplicationRepository;
import com.expo.recruitment.entity.RecruitmentNotice;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link BoothPaymentService} 의 결제 시작·승인 규칙을 확인한다. */
class BoothPaymentServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final Long CLIENT_USER_ID = 2L;
    private static final Long BOOTH_PRODUCT_ID = 3L;
    private static final Long APPLICATION_ID = 4L;
    private static final Long NOTICE_ID = 5L;
    private static final String PG_ORDER_ID = "BO12345-P1";

    private BoothPaymentRepository boothPaymentRepository;
    private BoothPaymentHistoryRepository boothPaymentHistoryRepository;
    private BoothOrderRepository boothOrderRepository;
    private BoothProductRepository boothProductRepository;
    private BoothReservationRepository boothReservationRepository;
    private ParticipationApplicationRepository participationApplicationRepository;
    private RecruitmentNoticeRepository recruitmentNoticeRepository;
    private BoothAllocationRepository boothAllocationRepository;
    private TossPaymentClient tossPaymentClient;
    private BoothPaymentService service;

    @BeforeEach
    void setUp() {
        boothPaymentRepository = mock(BoothPaymentRepository.class);
        boothPaymentHistoryRepository = mock(BoothPaymentHistoryRepository.class);
        boothOrderRepository = mock(BoothOrderRepository.class);
        boothProductRepository = mock(BoothProductRepository.class);
        boothReservationRepository = mock(BoothReservationRepository.class);
        participationApplicationRepository = mock(ParticipationApplicationRepository.class);
        recruitmentNoticeRepository = mock(RecruitmentNoticeRepository.class);
        boothAllocationRepository = mock(BoothAllocationRepository.class);
        tossPaymentClient = mock(TossPaymentClient.class);
        when(tossPaymentClient.getClientKey()).thenReturn("test_ck_dummy");
        BoothOrderCompletionService boothOrderCompletionService =
                new BoothOrderCompletionService(
                        boothProductRepository,
                        boothReservationRepository,
                        participationApplicationRepository,
                        boothAllocationRepository);
        service =
                new BoothPaymentService(
                        boothPaymentRepository,
                        boothPaymentHistoryRepository,
                        boothOrderRepository,
                        participationApplicationRepository,
                        recruitmentNoticeRepository,
                        boothOrderCompletionService,
                        tossPaymentClient,
                        new BoothPaymentConverter());
        when(participationApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(applicationForOrder()));
        when(recruitmentNoticeRepository.findById(NOTICE_ID)).thenReturn(Optional.of(openNotice()));
    }

    private ParticipationApplication applicationForOrder() {
        return ParticipationApplication.create(
                NOTICE_ID, CLIENT_USER_ID, "테스트 참가기업", null, null, BOOTH_PRODUCT_ID);
    }

    private RecruitmentNotice openNotice() {
        RecruitmentNotice notice =
                RecruitmentNotice.create(
                        10L,
                        20L,
                        "테스트 공고",
                        "내용",
                        Instant.now().minus(Duration.ofDays(1)),
                        Instant.now().plus(Duration.ofDays(1)),
                        99L);
        notice.publish();
        return notice;
    }

    private BoothOrder pendingOrder() {
        return BoothOrder.create(
                APPLICATION_ID,
                CLIENT_USER_ID,
                BOOTH_PRODUCT_ID,
                "BO12345",
                BigDecimal.valueOf(1_100_000),
                "idem-order",
                Instant.now().plus(Duration.ofMinutes(15)));
    }

    private BoothPayment readyPayment() {
        return BoothPayment.create(
                ORDER_ID, PG_ORDER_ID, BigDecimal.valueOf(1_100_000), "idem-payment");
    }

    /** {@code @GeneratedValue} id는 팩토리로 만든 엔티티엔 없어, DB에서 조회된 것처럼 리플렉션으로 채운다. */
    private static void withId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void initiateRejectsWhenOrderNotFound() {
        when(boothOrderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.initiate(ORDER_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ORDER_NOT_FOUND);
    }

    @Test
    void initiateRejectsWhenOrderExpired() {
        BoothOrder order =
                BoothOrder.create(
                        APPLICATION_ID,
                        CLIENT_USER_ID,
                        BOOTH_PRODUCT_ID,
                        "BO12345",
                        BigDecimal.valueOf(1_100_000),
                        "idem-order",
                        Instant.now().minus(Duration.ofMinutes(1)));
        when(boothOrderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.initiate(ORDER_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_ORDER_EXPIRED);
    }

    @Test
    void initiateRejectsWhenAlreadyApproved() {
        BoothPayment approved = readyPayment();
        approved.approve("payKey", "카드", BigDecimal.valueOf(1_100_000));
        when(boothOrderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(pendingOrder()));
        when(boothPaymentRepository.findAllByBoothOrderId(ORDER_ID)).thenReturn(List.of(approved));

        assertThatThrownBy(() -> service.initiate(ORDER_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_ALREADY_APPROVED);
    }

    @Test
    void initiateReusesExistingReadyPayment() {
        BoothPayment existing = readyPayment();
        when(boothOrderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(pendingOrder()));
        when(boothPaymentRepository.findAllByBoothOrderId(ORDER_ID)).thenReturn(List.of(existing));

        InitiateBoothPaymentResponse response = service.initiate(ORDER_ID, CLIENT_USER_ID);

        assertThat(response.pgOrderId()).isEqualTo(PG_ORDER_ID);
    }

    @Test
    void initiateCreatesNewAttemptWhenNoneExists() {
        when(boothOrderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(pendingOrder()));
        when(boothPaymentRepository.findAllByBoothOrderId(ORDER_ID)).thenReturn(List.of());
        when(boothPaymentRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InitiateBoothPaymentResponse response = service.initiate(ORDER_ID, CLIENT_USER_ID);

        assertThat(response.pgOrderId()).isEqualTo("BO12345-P1");
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(1_100_000));
    }

    @Test
    void confirmRejectsWhenPaymentNotFound() {
        when(boothPaymentRepository.findByPgOrderId(PG_ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.confirm(
                                        PG_ORDER_ID,
                                        "payKey",
                                        BigDecimal.valueOf(1_100_000),
                                        CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    void confirmRejectsWhenOrderNotOwnedByCaller() {
        when(boothPaymentRepository.findByPgOrderId(PG_ORDER_ID))
                .thenReturn(Optional.of(readyPayment()));
        when(boothOrderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(pendingOrder()));

        assertThatThrownBy(
                        () ->
                                service.confirm(
                                        PG_ORDER_ID, "payKey", BigDecimal.valueOf(1_100_000), 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ORDER_NOT_FOUND);
    }

    @Test
    void confirmRejectsWhenAmountMismatch() {
        when(boothPaymentRepository.findByPgOrderId(PG_ORDER_ID))
                .thenReturn(Optional.of(readyPayment()));
        when(boothOrderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(pendingOrder()));

        assertThatThrownBy(
                        () ->
                                service.confirm(
                                        PG_ORDER_ID,
                                        "payKey",
                                        BigDecimal.valueOf(999),
                                        CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    @Test
    void confirmRejectsWhenNoticeNotOpen() {
        RecruitmentNotice closedNotice = openNotice();
        closedNotice.close();
        when(boothPaymentRepository.findByPgOrderId(PG_ORDER_ID))
                .thenReturn(Optional.of(readyPayment()));
        when(boothOrderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(pendingOrder()));
        when(recruitmentNoticeRepository.findById(NOTICE_ID)).thenReturn(Optional.of(closedNotice));

        assertThatThrownBy(
                        () ->
                                service.confirm(
                                        PG_ORDER_ID,
                                        "payKey",
                                        BigDecimal.valueOf(1_100_000),
                                        CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_OPEN);
    }

    @Test
    void initiateRejectsWhenNoticeNotOpen() {
        RecruitmentNotice canceledNotice = openNotice();
        canceledNotice.cancel();
        when(boothOrderRepository.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(pendingOrder()));
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(canceledNotice));

        assertThatThrownBy(() -> service.initiate(ORDER_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_OPEN);
    }

    @Test
    void confirmSucceedsAndCascadesStatuses() {
        BoothPayment payment = readyPayment();
        BoothOrder order = pendingOrder();
        withId(order, ORDER_ID);
        BoothProduct product =
                BoothProduct.create(
                                NOTICE_ID,
                                10L,
                                BigDecimal.valueOf(1_000_000),
                                BigDecimal.valueOf(100_000),
                                true,
                                null)
                        .schedule(null, null, true);
        product.reserve();
        BoothReservation reservation =
                BoothReservation.create(BOOTH_PRODUCT_ID, ORDER_ID, CLIENT_USER_ID, Instant.now());
        ParticipationApplication application =
                ParticipationApplication.create(
                        NOTICE_ID, CLIENT_USER_ID, "테스트 참가기업", null, null, BOOTH_PRODUCT_ID);
        application.startPayment(ORDER_ID);

        when(boothPaymentRepository.findByPgOrderId(PG_ORDER_ID)).thenReturn(Optional.of(payment));
        when(boothOrderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(tossPaymentClient.confirmPayment(anyString(), anyString(), any(), anyString()))
                .thenReturn(
                        new TossConfirmResult(
                                "payKey",
                                PG_ORDER_ID,
                                "DONE",
                                "카드",
                                BigDecimal.valueOf(1_100_000),
                                Instant.now(),
                                "{}"));
        when(boothProductRepository.findById(BOOTH_PRODUCT_ID)).thenReturn(Optional.of(product));
        when(boothReservationRepository.findFirstByBoothOrderIdAndStatus(
                        ORDER_ID, BoothReservationStatus.ACTIVE))
                .thenReturn(Optional.of(reservation));
        when(participationApplicationRepository.findByIdAndClientUserId(
                        APPLICATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(application));

        BoothPaymentResponse response =
                service.confirm(
                        PG_ORDER_ID, "payKey", BigDecimal.valueOf(1_100_000), CLIENT_USER_ID);

        assertThat(response.status()).isEqualTo(BoothPaymentStatus.APPROVED);
        assertThat(order.getStatus().name()).isEqualTo("PAYMENT_COMPLETED");
        assertThat(product.getSalesStatus()).isEqualTo(BoothSalesStatus.SOLD);
        assertThat(reservation.getStatus()).isEqualTo(BoothReservationStatus.CONFIRMED);
        assertThat(application.getStatus().name()).isEqualTo("SUBMITTED");
        verify(boothAllocationRepository).save(any());
    }

    @Test
    void confirmHandlesTossFailureAndLeavesOrderPending() {
        BoothPayment payment = readyPayment();
        BoothOrder order = pendingOrder();

        when(boothPaymentRepository.findByPgOrderId(PG_ORDER_ID)).thenReturn(Optional.of(payment));
        when(boothOrderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(tossPaymentClient.confirmPayment(anyString(), anyString(), any(), anyString()))
                .thenThrow(new TossApiException("REJECT_CARD_COMPANY", "카드사 거절", "{}"));

        assertThatThrownBy(
                        () ->
                                service.confirm(
                                        PG_ORDER_ID,
                                        "payKey",
                                        BigDecimal.valueOf(1_100_000),
                                        CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_APPROVAL_FAILED);

        assertThat(payment.getStatus()).isEqualTo(BoothPaymentStatus.FAILED);
        assertThat(payment.getLastFailureCode()).isEqualTo("REJECT_CARD_COMPANY");
        assertThat(order.getStatus().name()).isEqualTo("PENDING_PAYMENT");
    }
}
