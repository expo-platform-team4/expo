package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.entity.BoothOrder;
import com.expo.booth.entity.BoothOrderStatus;
import com.expo.booth.entity.BoothProduct;
import com.expo.booth.entity.BoothReservation;
import com.expo.booth.entity.BoothReservationStatus;
import com.expo.booth.entity.BoothSalesStatus;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothProductRepository;
import com.expo.booth.repository.BoothReservationRepository;
import com.expo.participation.entity.ParticipationApplication;
import com.expo.participation.repository.ParticipationApplicationRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link BoothOrderCompletionService} 가 결제 승인 후 관련 엔티티 상태를 전부 전파하는지 확인한다. */
class BoothOrderCompletionServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final Long CLIENT_USER_ID = 2L;
    private static final Long BOOTH_PRODUCT_ID = 3L;
    private static final Long APPLICATION_ID = 4L;
    private static final Long NOTICE_ID = 5L;

    private BoothProductRepository boothProductRepository;
    private BoothReservationRepository boothReservationRepository;
    private ParticipationApplicationRepository participationApplicationRepository;
    private BoothAllocationRepository boothAllocationRepository;
    private BoothOrderCompletionService service;

    @BeforeEach
    void setUp() {
        boothProductRepository = mock(BoothProductRepository.class);
        boothReservationRepository = mock(BoothReservationRepository.class);
        participationApplicationRepository = mock(ParticipationApplicationRepository.class);
        boothAllocationRepository = mock(BoothAllocationRepository.class);
        service =
                new BoothOrderCompletionService(
                        boothProductRepository,
                        boothReservationRepository,
                        participationApplicationRepository,
                        boothAllocationRepository);
    }

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
    void completePropagatesOrderProductReservationApplicationAndAllocation() {
        BoothOrder order =
                BoothOrder.create(
                        APPLICATION_ID,
                        CLIENT_USER_ID,
                        BOOTH_PRODUCT_ID,
                        "BO12345",
                        BigDecimal.valueOf(1_100_000),
                        "idem-order",
                        LocalDateTime.now().plusMinutes(15));
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
                BoothReservation.create(
                        BOOTH_PRODUCT_ID, ORDER_ID, CLIENT_USER_ID, LocalDateTime.now());
        ParticipationApplication application =
                ParticipationApplication.create(
                        NOTICE_ID, CLIENT_USER_ID, "테스트 참가기업", null, null, BOOTH_PRODUCT_ID);
        application.startPayment(ORDER_ID);

        when(boothProductRepository.findById(BOOTH_PRODUCT_ID)).thenReturn(Optional.of(product));
        when(boothReservationRepository.findFirstByBoothOrderIdAndStatus(
                        ORDER_ID, BoothReservationStatus.ACTIVE))
                .thenReturn(Optional.of(reservation));
        when(participationApplicationRepository.findByIdAndClientUserId(
                        APPLICATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(application));

        service.complete(order);

        assertThat(order.getStatus()).isEqualTo(BoothOrderStatus.PAYMENT_COMPLETED);
        assertThat(order.getPaidAt()).isNotNull();
        assertThat(product.getSalesStatus()).isEqualTo(BoothSalesStatus.SOLD);
        assertThat(reservation.getStatus()).isEqualTo(BoothReservationStatus.CONFIRMED);
        assertThat(application.getStatus().name()).isEqualTo("SUBMITTED");
        verify(boothAllocationRepository)
                .save(
                        argThat(
                                (BoothAllocation allocation) ->
                                        allocation.getApplicationId().equals(APPLICATION_ID)
                                                && allocation.getBoothOrderId().equals(ORDER_ID)
                                                && allocation
                                                        .getBoothProductId()
                                                        .equals(BOOTH_PRODUCT_ID)
                                                && allocation
                                                        .getClientUserId()
                                                        .equals(CLIENT_USER_ID)));
    }
}
