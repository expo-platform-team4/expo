package com.expo.payment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.config.TossCancelResult;
import com.expo.common.config.TossConfirmResult;
import com.expo.common.config.TossPaymentClient;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.payment.dto.ConfirmTicketPaymentRequest;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.repository.TicketPaymentRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 토스 승인 뒤 로컬 확정 실패 시 PG 취소·보상 연결을 확인한다. */
class TicketPaymentConfirmationServiceTest {
    private static final Long PAYMENT_ID = 1L;
    private static final Long ORDER_ID = 2L;

    private TicketPaymentConfirmationPreparationService preparation;
    private TicketPaymentCompletionService completion;
    private TicketPaymentRepository payments;
    private TossPaymentClient tossPaymentClient;
    private TicketPaymentCompensationService compensation;
    private TicketPaymentConfirmationService service;

    @BeforeEach
    void setUp() {
        preparation = mock(TicketPaymentConfirmationPreparationService.class);
        completion = mock(TicketPaymentCompletionService.class);
        payments = mock(TicketPaymentRepository.class);
        tossPaymentClient = mock(TossPaymentClient.class);
        compensation = mock(TicketPaymentCompensationService.class);
        service =
                new TicketPaymentConfirmationService(
                        preparation,
                        completion,
                        mock(TicketPaymentFailureService.class),
                        compensation,
                        payments,
                        tossPaymentClient);
    }

    @Test
    void confirmCancelsTossPaymentWhenLocalCompletionFails() {
        ConfirmTicketPaymentRequest request =
                new ConfirmTicketPaymentRequest("payment-key", "TICKET-P1", BigDecimal.TEN);
        TicketPayment payment = payment();
        TicketPaymentConfirmationTarget target =
                new TicketPaymentConfirmationTarget(ORDER_ID, PAYMENT_ID, null);
        TossConfirmResult confirmResult =
                new TossConfirmResult(
                        "payment-key",
                        "TICKET-P1",
                        "DONE",
                        "CARD",
                        BigDecimal.TEN,
                        Instant.now(),
                        "{}");
        TossCancelResult cancelResult =
                new TossCancelResult("cancel-key", BigDecimal.TEN, Instant.now(), "{}");
        when(preparation.prepare(request.orderId(), request.amount(), null)).thenReturn(target);
        when(payments.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(tossPaymentClient.confirmPayment(any(), any(), any(), any()))
                .thenReturn(confirmResult);
        when(completion.complete(target, confirmResult, null))
                .thenThrow(new BusinessException(ErrorCode.PAYMENT_ORDER_EXPIRED));
        when(tossPaymentClient.cancelPayment(any(), any(), any())).thenReturn(cancelResult);

        assertThatThrownBy(() -> service.confirm(request, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_ORDER_EXPIRED);
        verify(tossPaymentClient).cancelPayment("payment-key", "로컬 결제 확정 실패", "idem-cancel");
        verify(compensation).compensate(PAYMENT_ID, cancelResult);
    }

    private TicketPayment payment() {
        TicketPayment payment =
                TicketPayment.create(
                        ORDER_ID,
                        "TICKET-P1",
                        BigDecimal.TEN,
                        BigDecimal.TEN,
                        BigDecimal.ZERO,
                        "idem");
        try {
            Field field = TicketPayment.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(payment, PAYMENT_ID);
            return payment;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
