package com.expo.refund.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.entity.TicketPaymentStatus;
import com.expo.payment.repository.TicketPaymentRepository;
import com.expo.refund.entity.TicketRefund;
import com.expo.refund.entity.TicketRefundStatus;
import com.expo.refund.repository.TicketRefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 토스 취소 전에 환불 행을 PROCESSING으로 전이한다. */
@Service
@RequiredArgsConstructor
class TicketRefundExecutionPreparationService {

    private static final String DEFAULT_CANCEL_REASON = "티켓 주문 환불";

    private final TicketRefundRepository ticketRefundRepository;
    private final TicketPaymentRepository ticketPaymentRepository;

    @Transactional
    public TicketRefundExecutionTarget prepare(Long refundId) {
        TicketRefund refund =
                ticketRefundRepository
                        .findByIdForUpdate(refundId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.REFUND_ALREADY_REQUESTED));
        if (refund.getStatus() == TicketRefundStatus.COMPLETED) {
            return new TicketRefundExecutionTarget(refundId, null, null, null, null, true);
        }
        TicketPayment payment =
                ticketPaymentRepository
                        .findById(refund.getTicketPaymentId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.REFUND_PAYMENT_NOT_COMPLETED));
        if (payment.getStatus() != TicketPaymentStatus.DONE || payment.getPaymentKey() == null) {
            throw new BusinessException(ErrorCode.REFUND_PAYMENT_NOT_COMPLETED);
        }
        refund.markProcessing();
        String cancelReason =
                refund.getReason() == null || refund.getReason().isBlank()
                        ? DEFAULT_CANCEL_REASON
                        : refund.getReason();
        return new TicketRefundExecutionTarget(
                refund.getId(),
                payment.getId(),
                payment.getPaymentKey(),
                cancelReason,
                "ticket-refund-" + refund.getId(),
                false);
    }
}
