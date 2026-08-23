package com.expo.refund.service;

import com.expo.checkin.repository.IssuedTicketRepository;
import com.expo.common.config.TossCancelResult;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.entity.TicketPaymentEventType;
import com.expo.payment.entity.TicketPaymentHistory;
import com.expo.payment.entity.TicketPaymentStatus;
import com.expo.payment.repository.TicketPaymentHistoryRepository;
import com.expo.payment.repository.TicketPaymentRepository;
import com.expo.refund.entity.TicketRefund;
import com.expo.refund.entity.TicketRefundStatus;
import com.expo.refund.repository.TicketRefundRepository;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.repository.TicketInventoryRepository;
import com.expo.ticket.repository.TicketOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class TicketRefundCompletionService {
    private final TicketRefundRepository refunds;
    private final TicketPaymentRepository payments;
    private final TicketPaymentHistoryRepository histories;
    private final TicketOrderRepository orders;
    private final TicketInventoryRepository inventories;
    private final IssuedTicketRepository issuedTickets;

    @Transactional
    public void complete(TicketRefundExecutionTarget target, TossCancelResult result) {
        TicketRefund refund = refunds.findByIdForUpdate(target.refundId()).orElseThrow();
        if (refund.getStatus() == TicketRefundStatus.COMPLETED) {
            return;
        }
        TicketOrder order = orders.findByIdForUpdate(refund.getTicketOrderId()).orElseThrow();
        TicketPayment payment = payments.findById(target.paymentId()).orElseThrow();
        issuedTickets
                .findAllByTicketOrderIdForUpdate(order.getId())
                .forEach(ticket -> ticket.cancel());
        order.getItems()
                .forEach(
                        item ->
                                inventories.restoreSold(
                                        item.getTicketProduct().getId(), item.getQuantity()));
        TicketPaymentStatus before = payment.getStatus();
        payment.cancel(result.cancelAmount());
        order.cancel();
        refund.complete(result.transactionKey());
        histories.save(
                TicketPaymentHistory.record(
                        payment.getId(),
                        TicketPaymentEventType.CANCEL,
                        before,
                        TicketPaymentStatus.CANCELED,
                        result.cancelAmount(),
                        result.transactionKey(),
                        result.rawResponse()));
    }
}
