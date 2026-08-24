package com.expo.payment.service;

import com.expo.common.config.TossCancelResult;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.entity.TicketPaymentEventType;
import com.expo.payment.entity.TicketPaymentHistory;
import com.expo.payment.entity.TicketPaymentStatus;
import com.expo.payment.repository.TicketPaymentHistoryRepository;
import com.expo.payment.repository.TicketPaymentRepository;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.repository.TicketOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PG 승인 뒤 로컬 확정이 실패했을 때 취소 결과를 기록한다. */
@Service
@RequiredArgsConstructor
class TicketPaymentCompensationService {
    private final TicketPaymentRepository payments;
    private final TicketPaymentHistoryRepository histories;
    private final TicketOrderRepository orders;

    @Transactional
    public void compensate(Long paymentId, TossCancelResult result) {
        TicketPayment payment = payments.findById(paymentId).orElseThrow();
        if (payment.getStatus() == TicketPaymentStatus.CANCELED) {
            return;
        }
        TicketOrder order = orders.findByIdForUpdate(payment.getTicketOrderId()).orElseThrow();
        TicketPaymentStatus before = payment.getStatus();
        payment.cancel(result.cancelAmount());
        order.markPaymentFailed();
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
