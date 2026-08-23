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
import com.expo.payment.dto.TicketPaymentStatusResponse;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.repository.TicketPaymentRepository;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.repository.TicketOrderRepository;
import com.expo.ticket.service.TicketOrderAccessVerifier;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link TicketPaymentStatusService}의 주문 소유자 상태 조회를 확인한다. */
class TicketPaymentStatusServiceTest {

    private static final String ORDER_NUMBER = "TICKET-test";
    private static final Long ORDER_ID = 1L;

    private TicketOrderRepository ticketOrderRepository;
    private TicketPaymentRepository ticketPaymentRepository;
    private TicketOrderAccessVerifier ticketOrderAccessVerifier;
    private TicketPaymentConverter ticketPaymentConverter;
    private TicketPaymentStatusService service;

    @BeforeEach
    void setUp() {
        ticketOrderRepository = mock(TicketOrderRepository.class);
        ticketPaymentRepository = mock(TicketPaymentRepository.class);
        ticketOrderAccessVerifier = mock(TicketOrderAccessVerifier.class);
        ticketPaymentConverter = mock(TicketPaymentConverter.class);
        service =
                new TicketPaymentStatusService(
                        ticketOrderRepository,
                        ticketPaymentRepository,
                        ticketOrderAccessVerifier,
                        ticketPaymentConverter);
    }

    @Test
    void getStatusDoesNotRevealWhetherUnknownOrderExists() {
        when(ticketOrderRepository.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStatus(ORDER_NUMBER, mock(AuthPrincipal.class)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_ORDER_ACCESS_DENIED);
    }

    @Test
    void getStatusReturnsOrderOnlyResponseBeforePaymentInitiation() {
        TicketOrder order = order();
        AuthPrincipal principal = mock(AuthPrincipal.class);
        TicketPaymentStatusResponse response = mock(TicketPaymentStatusResponse.class);
        when(ticketOrderRepository.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.of(order));
        when(ticketPaymentRepository.findByTicketOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(ticketPaymentConverter.toStatusResponse(order)).thenReturn(response);

        TicketPaymentStatusResponse result = service.getStatus(ORDER_NUMBER, principal);

        assertThat(result).isSameAs(response);
        verify(ticketOrderAccessVerifier).verifyForPaymentFlow(order, principal);
    }

    @Test
    void getStatusReturnsPaymentResponseWhenPaymentExists() {
        TicketOrder order = order();
        TicketPayment payment =
                TicketPayment.create(
                        ORDER_ID,
                        "TICKET-test-P1",
                        BigDecimal.valueOf(10_300),
                        BigDecimal.valueOf(10_000),
                        BigDecimal.valueOf(300),
                        "idempotency");
        TicketPaymentStatusResponse response = mock(TicketPaymentStatusResponse.class);
        when(ticketOrderRepository.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.of(order));
        when(ticketPaymentRepository.findByTicketOrderId(ORDER_ID))
                .thenReturn(Optional.of(payment));
        when(ticketPaymentConverter.toStatusResponse(order, payment)).thenReturn(response);

        TicketPaymentStatusResponse result =
                service.getStatus(ORDER_NUMBER, mock(AuthPrincipal.class));

        assertThat(result).isSameAs(response);
    }

    private TicketOrder order() {
        TicketOrder order =
                TicketOrder.creatGuestOrder(
                        ORDER_NUMBER,
                        BigDecimal.valueOf(10_000),
                        BigDecimal.valueOf(0.03),
                        BigDecimal.valueOf(300),
                        BigDecimal.valueOf(10_300),
                        1);
        try {
            Field field = TicketOrder.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(order, ORDER_ID);
            return order;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
