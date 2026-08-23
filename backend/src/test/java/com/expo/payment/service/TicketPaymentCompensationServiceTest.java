package com.expo.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.expo.common.config.TossCancelResult;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.entity.TicketPaymentStatus;
import com.expo.payment.repository.TicketPaymentHistoryRepository;
import com.expo.payment.repository.TicketPaymentRepository;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrderStatus;
import com.expo.ticket.repository.TicketOrderRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link TicketPaymentCompensationService}의 PG 취소 후 로컬 보상 상태 전이를 확인한다. */
class TicketPaymentCompensationServiceTest {
    private static final Long PAYMENT_ID = 1L;
    private static final Long ORDER_ID = 2L;

    private TicketPaymentRepository payments;
    private TicketPaymentHistoryRepository histories;
    private TicketOrderRepository orders;
    private TicketPaymentCompensationService service;

    @BeforeEach
    void setUp() {
        payments = mock(TicketPaymentRepository.class);
        histories = mock(TicketPaymentHistoryRepository.class);
        orders = mock(TicketOrderRepository.class);
        service = new TicketPaymentCompensationService(payments, histories, orders);
    }

    @Test
    void compensateCancelsPaymentAndMarksOrderPaymentFailed() {
        TicketPayment payment = payment();
        TicketOrder order = order();
        when(payments.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(orders.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        service.compensate(PAYMENT_ID, cancelResult());

        assertThat(payment.getStatus()).isEqualTo(TicketPaymentStatus.CANCELED);
        assertThat(payment.getCanceledAmount()).isEqualByComparingTo(BigDecimal.valueOf(10_300));
        assertThat(order.getStatus()).isEqualTo(TicketOrderStatus.PAYMENT_FAILED);
    }

    @Test
    void compensateDoesNothingForAlreadyCanceledPayment() {
        TicketPayment payment = payment();
        payment.cancel(BigDecimal.valueOf(10_300));
        when(payments.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        service.compensate(PAYMENT_ID, cancelResult());

        verifyNoInteractions(orders, histories);
    }

    private TicketPayment payment() {
        TicketPayment payment =
                TicketPayment.create(
                        ORDER_ID,
                        "TICKET-test-P1",
                        BigDecimal.valueOf(10_300),
                        BigDecimal.valueOf(10_000),
                        BigDecimal.valueOf(300),
                        "idempotency");
        setField(payment, "id", PAYMENT_ID);
        return payment;
    }

    private TicketOrder order() {
        TicketOrder order =
                TicketOrder.creatGuestOrder(
                        "TICKET-test",
                        BigDecimal.valueOf(10_000),
                        BigDecimal.valueOf(0.03),
                        BigDecimal.valueOf(300),
                        BigDecimal.valueOf(10_300),
                        1);
        setField(order, "id", ORDER_ID);
        return order;
    }

    private TossCancelResult cancelResult() {
        return new TossCancelResult("cancel-key", BigDecimal.valueOf(10_300), Instant.now(), "{}");
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
