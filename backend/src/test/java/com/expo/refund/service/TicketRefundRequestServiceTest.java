package com.expo.refund.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.jwt.AuthPrincipal;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.repository.TicketPaymentRepository;
import com.expo.refund.converter.TicketRefundConverter;
import com.expo.refund.dto.TicketRefundResponse;
import com.expo.refund.entity.TicketRefund;
import com.expo.refund.repository.TicketRefundRepository;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.repository.TicketOrderRepository;
import com.expo.ticket.service.TicketOrderAccessVerifier;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/** 회원·비회원 티켓 환불 요청의 주문·결제 상태 검증을 확인한다. */
class TicketRefundRequestServiceTest {

    private static final Long ORDER_ID = 1L;

    private TicketOrderRepository orders;
    private TicketPaymentRepository payments;
    private TicketRefundRepository refunds;
    private TicketRefundConverter converter;
    private TicketRefundRequestService service;

    @BeforeEach
    void setUp() {
        orders = mock(TicketOrderRepository.class);
        payments = mock(TicketPaymentRepository.class);
        refunds = mock(TicketRefundRepository.class);
        converter = mock(TicketRefundConverter.class);
        service =
                new TicketRefundRequestService(
                        orders,
                        payments,
                        refunds,
                        mock(TicketOrderAccessVerifier.class),
                        converter,
                        mock(ApplicationEventPublisher.class));
    }

    @Test
    void requestGuestRefundCreatesRequestedRefundForPaidOrder() {
        TicketOrder order = paidGuestOrder();
        TicketPayment payment = approvedPayment();
        TicketRefundResponse response = mock(TicketRefundResponse.class);
        when(orders.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(refunds.findByTicketOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(payments.findByTicketOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
        when(refunds.save(any(TicketRefund.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(converter.toResponse(any(TicketRefund.class))).thenReturn(response);

        TicketRefundResponse result = service.requestGuestRefund(ORDER_ID, "단순 변심");

        assertThat(result).isSameAs(response);
    }

    @Test
    void requestGuestRefundRejectsDuplicateRequest() {
        TicketOrder order = paidGuestOrder();
        when(orders.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(refunds.findByTicketOrderId(ORDER_ID))
                .thenReturn(Optional.of(mock(TicketRefund.class)));

        assertThatThrownBy(() -> service.requestGuestRefund(ORDER_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFUND_ALREADY_REQUESTED);
    }

    @Test
    void requestMemberRefundRejectsPendingOrder() {
        TicketOrder order =
                TicketOrder.createMemberOrder(
                        "TICKET-test",
                        10L,
                        BigDecimal.TEN,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.TEN,
                        1);
        setField(order, "id", ORDER_ID);
        when(orders.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(
                        () ->
                                service.requestMemberRefund(
                                        ORDER_ID, null, mock(AuthPrincipal.class)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFUND_ORDER_NOT_PAID);
    }

    private TicketOrder paidGuestOrder() {
        TicketOrder order =
                TicketOrder.creatGuestOrder(
                        "TICKET-test",
                        BigDecimal.valueOf(10_000),
                        BigDecimal.valueOf(0.03),
                        BigDecimal.valueOf(300),
                        BigDecimal.valueOf(10_300),
                        1);
        setField(order, "id", ORDER_ID);
        order.markPaid();
        return order;
    }

    private TicketPayment approvedPayment() {
        TicketPayment payment =
                TicketPayment.create(
                        ORDER_ID,
                        "TICKET-test-P1",
                        BigDecimal.valueOf(10_300),
                        BigDecimal.valueOf(10_000),
                        BigDecimal.valueOf(300),
                        "idempotency");
        setField(payment, "id", 2L);
        payment.approve("paymentKey", "CARD", BigDecimal.valueOf(10_300));
        return payment;
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
