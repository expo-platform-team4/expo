package com.expo.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.jwt.AuthPrincipal;
import com.expo.payment.converter.TicketPaymentConverter;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.repository.TicketPaymentRepository;
import com.expo.ticket.entity.InventoryReservation;
import com.expo.ticket.entity.TicketInventory;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.repository.InventoryReservationRepository;
import com.expo.ticket.repository.TicketOrderRepository;
import com.expo.ticket.service.TicketOrderAccessVerifier;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link TicketPaymentConfirmationPreparationService}의 승인 전 검증과 예약 연장을 확인한다. */
class TicketPaymentConfirmationPreparationServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final String PG_ORDER_ID = "TICKET-test-P1";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(10_300);

    private TicketPaymentRepository ticketPaymentRepository;
    private TicketOrderRepository ticketOrderRepository;
    private InventoryReservationRepository inventoryReservationRepository;
    private TicketPaymentConfirmationPreparationService service;

    @BeforeEach
    void setUp() {
        ticketPaymentRepository = mock(TicketPaymentRepository.class);
        ticketOrderRepository = mock(TicketOrderRepository.class);
        inventoryReservationRepository = mock(InventoryReservationRepository.class);
        service =
                new TicketPaymentConfirmationPreparationService(
                        ticketPaymentRepository,
                        ticketOrderRepository,
                        inventoryReservationRepository,
                        mock(TicketOrderAccessVerifier.class),
                        mock(TicketPaymentConverter.class));
    }

    @Test
    void prepareExtendsActiveReservationBeforeTossApproval() {
        TicketOrder order = order();
        TicketPayment payment = payment();
        InventoryReservation reservation = reservation(order);
        Instant previousExpiration = reservation.getExpiresAt();
        givenPaymentAndOrder(payment, order);
        when(inventoryReservationRepository.findAllByTicketOrderIdForUpdate(ORDER_ID))
                .thenReturn(List.of(reservation));

        TicketPaymentConfirmationTarget target =
                service.prepare(PG_ORDER_ID, AMOUNT, mock(AuthPrincipal.class));

        assertThat(target.ticketOrderId()).isEqualTo(ORDER_ID);
        assertThat(target.ticketPaymentId()).isEqualTo(payment.getId());
        assertThat(reservation.getExpiresAt()).isAfter(previousExpiration);
    }

    @Test
    void prepareRejectsCanceledPaymentBeforeCallingToss() {
        TicketOrder order = order();
        TicketPayment payment = payment();
        payment.cancel(BigDecimal.ZERO);
        givenPaymentAndOrder(payment, order);

        assertThatThrownBy(() -> service.prepare(PG_ORDER_ID, AMOUNT, mock(AuthPrincipal.class)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_ORDER_NOT_PENDING);
    }

    @Test
    void prepareRejectsExpiredReservationBeforeCallingToss() {
        TicketOrder order = order();
        TicketPayment payment = payment();
        InventoryReservation reservation = reservation(order);
        setField(reservation, "expiresAt", Instant.now().minusSeconds(1));
        givenPaymentAndOrder(payment, order);
        when(inventoryReservationRepository.findAllByTicketOrderIdForUpdate(ORDER_ID))
                .thenReturn(List.of(reservation));

        assertThatThrownBy(() -> service.prepare(PG_ORDER_ID, AMOUNT, mock(AuthPrincipal.class)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_ORDER_EXPIRED);
    }

    private void givenPaymentAndOrder(TicketPayment payment, TicketOrder order) {
        when(ticketPaymentRepository.findByPgOrderId(PG_ORDER_ID)).thenReturn(Optional.of(payment));
        when(ticketOrderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
    }

    private TicketOrder order() {
        TicketOrder order =
                TicketOrder.creatGuestOrder(
                        "TICKET-test",
                        BigDecimal.valueOf(10_000),
                        BigDecimal.valueOf(0.03),
                        BigDecimal.valueOf(300),
                        AMOUNT,
                        1);
        setField(order, "id", ORDER_ID);
        return order;
    }

    private TicketPayment payment() {
        TicketPayment payment =
                TicketPayment.create(
                        ORDER_ID,
                        PG_ORDER_ID,
                        AMOUNT,
                        BigDecimal.valueOf(10_000),
                        BigDecimal.valueOf(300),
                        "idempotency");
        setField(payment, "id", 2L);
        return payment;
    }

    private InventoryReservation reservation(TicketOrder order) {
        TicketProduct product =
                TicketProduct.create(
                        1L,
                        "1일권",
                        null,
                        BigDecimal.valueOf(10_000),
                        Instant.now().minusSeconds(60),
                        Instant.now().plusSeconds(3600),
                        4);
        product.attachInventory(TicketInventory.create(product, 10));
        return InventoryReservation.builder()
                .ticketOrder(order)
                .ticketProduct(product)
                .quantity(1)
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
