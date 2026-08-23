package com.expo.refund.event;

import com.expo.refund.service.TicketRefundExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 환불 요청 커밋 후 PG 취소를 실행한다. */
@Component
@RequiredArgsConstructor
public class TicketRefundRequestedListener {

    private final TicketRefundExecutionService ticketRefundExecutionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void execute(TicketRefundRequestedEvent event) {
        ticketRefundExecutionService.execute(event.refundId());
    }
}
