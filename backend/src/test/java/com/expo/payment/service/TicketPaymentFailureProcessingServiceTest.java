package com.expo.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.jwt.AuthPrincipal;
import com.expo.payment.converter.TicketPaymentConverter;
import com.expo.payment.dto.FailTicketPaymentResponse;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.entity.TicketPaymentHistory;
import com.expo.payment.entity.TicketPaymentStatus;
import com.expo.payment.repository.TicketPaymentHistoryRepository;
import com.expo.payment.repository.TicketPaymentRepository;
import com.expo.ticket.entity.InventoryReservation;
import com.expo.ticket.entity.InventoryReservationStatus;
import com.expo.ticket.entity.TicketInventory;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrderStatus;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.repository.InventoryReservationRepository;
import com.expo.ticket.repository.TicketInventoryRepository;
import com.expo.ticket.repository.TicketOrderRepository;
import com.expo.ticket.service.TicketOrderAccessVerifier;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link TicketPaymentFailureProcessingService}의 재고 반환·상태 전이를 확인한다. */
class TicketPaymentFailureProcessingServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final Long PRODUCT_ID = 2L;
    private static final String PG_ORDER_ID = "TICKET-test-P1";

    private TicketPaymentRepository ticketPaymentRepository;
    private TicketPaymentHistoryRepository ticketPaymentHistoryRepository;
    private TicketOrderRepository ticketOrderRepository;
    private InventoryReservationRepository inventoryReservationRepository;
    private TicketInventoryRepository ticketInventoryRepository;
    private TicketOrderAccessVerifier ticketOrderAccessVerifier;
    private TicketPaymentConverter ticketPaymentConverter;
    private TicketPaymentFailureProcessingService service;

    @BeforeEach
    void setUp() {
        ticketPaymentRepository = mock(TicketPaymentRepository.class);
        ticketPaymentHistoryRepository = mock(TicketPaymentHistoryRepository.class);
        ticketOrderRepository = mock(TicketOrderRepository.class);
        inventoryReservationRepository = mock(InventoryReservationRepository.class);
        ticketInventoryRepository = mock(TicketInventoryRepository.class);
        ticketOrderAccessVerifier = mock(TicketOrderAccessVerifier.class);
        ticketPaymentConverter = mock(TicketPaymentConverter.class);
        service =
                new TicketPaymentFailureProcessingService(
                        ticketPaymentRepository,
                        ticketPaymentHistoryRepository,
                        ticketOrderRepository,
                        inventoryReservationRepository,
                        ticketInventoryRepository,
                        ticketOrderAccessVerifier,
                        ticketPaymentConverter);
    }

    @Test
    void processReleasesReservationAndRecordsFailure() {
        TicketOrder order = order();
        TicketPayment payment = payment();
        InventoryReservation reservation = reservation(order, product());
        FailTicketPaymentResponse response = mock(FailTicketPaymentResponse.class);
        givenPendingPayment(order, payment);
        when(inventoryReservationRepository.findAllByTicketOrderId(ORDER_ID))
                .thenReturn(List.of(reservation));
        when(ticketInventoryRepository.releaseReserved(PRODUCT_ID, 2)).thenReturn(1);
        when(ticketPaymentConverter.toFailureResponse(order, payment)).thenReturn(response);

        FailTicketPaymentResponse result =
                service.process(PG_ORDER_ID, "PAYMENT_CANCELED", mock(AuthPrincipal.class));

        assertThat(result).isSameAs(response);
        assertThat(payment.getStatus()).isEqualTo(TicketPaymentStatus.FAILED);
        assertThat(order.getStatus()).isEqualTo(TicketOrderStatus.PAYMENT_FAILED);
        assertThat(reservation.getStatus()).isEqualTo(InventoryReservationStatus.RELEASED);
        verify(ticketPaymentHistoryRepository)
                .save(org.mockito.ArgumentMatchers.any(TicketPaymentHistory.class));
    }

    @Test
    void processRejectsWhenPaymentWasAlreadyApproved() {
        TicketOrder order = order();
        TicketPayment payment = payment();
        payment.approve("paymentKey", "CARD", BigDecimal.valueOf(10_300));
        givenPendingPayment(order, payment);

        assertThatThrownBy(() -> service.process(PG_ORDER_ID, "FAIL", mock(AuthPrincipal.class)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_ALREADY_APPROVED);
    }

    @Test
    void processRejectsWhenReservedStockCannotBeReturned() {
        TicketOrder order = order();
        TicketPayment payment = payment();
        InventoryReservation reservation = reservation(order, product());
        givenPendingPayment(order, payment);
        when(inventoryReservationRepository.findAllByTicketOrderId(ORDER_ID))
                .thenReturn(List.of(reservation));
        when(ticketInventoryRepository.releaseReserved(PRODUCT_ID, 2)).thenReturn(0);

        assertThatThrownBy(() -> service.process(PG_ORDER_ID, "FAIL", mock(AuthPrincipal.class)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_RESERVATION_NOT_FOUND);
    }

    private void givenPendingPayment(TicketOrder order, TicketPayment payment) {
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
                        BigDecimal.valueOf(10_300),
                        2);
        setField(order, "id", ORDER_ID);
        return order;
    }

    private TicketPayment payment() {
        TicketPayment payment =
                TicketPayment.create(
                        ORDER_ID,
                        PG_ORDER_ID,
                        BigDecimal.valueOf(10_300),
                        BigDecimal.valueOf(10_000),
                        BigDecimal.valueOf(300),
                        "idempotency");
        setField(payment, "id", 3L);
        return payment;
    }

    private TicketProduct product() {
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
        setField(product, "id", PRODUCT_ID);
        return product;
    }

    private InventoryReservation reservation(TicketOrder order, TicketProduct product) {
        return InventoryReservation.builder()
                .ticketOrder(order)
                .ticketProduct(product)
                .quantity(2)
                .expiresAt(Instant.now().plusSeconds(600))
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
