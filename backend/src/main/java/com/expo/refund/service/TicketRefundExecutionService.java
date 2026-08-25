package com.expo.refund.service;

import com.expo.common.config.TossApiException;
import com.expo.common.config.TossCancelResult;
import com.expo.common.config.TossPaymentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/** 환불 요청을 토스 취소와 로컬 주문 취소로 완료한다. HTTP로 노출하지 않는다. */
@Service
@RequiredArgsConstructor
public class TicketRefundExecutionService {

    private final TicketRefundExecutionPreparationService preparationService;
    private final TicketRefundExecutionFailureService failureService;
    private final TossPaymentClient tossPaymentClient;
    private final TicketRefundCompletionService ticketRefundCompletionService;

    public void execute(Long refundId) {
        TicketRefundExecutionTarget target = preparationService.prepare(refundId);
        if (target.alreadyCompleted()) {
            return;
        }
        try {
            TossCancelResult result =
                    tossPaymentClient.cancelPayment(
                            target.paymentKey(), target.cancelReason(), target.idempotencyKey());
            ticketRefundCompletionService.complete(target, result);
        } catch (TossApiException e) {
            failureService.recordFailure(refundId, e.getCode());
            throw e;
        } catch (DataAccessException e) {
            failureService.recordFailure(refundId, "EXECUTION_ERROR");
            throw e;
        }
    }
}
