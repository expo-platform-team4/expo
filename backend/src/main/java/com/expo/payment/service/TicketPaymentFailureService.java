package com.expo.payment.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.entity.TicketPaymentEventType;
import com.expo.payment.entity.TicketPaymentHistory;
import com.expo.payment.entity.TicketPaymentStatus;
import com.expo.payment.repository.TicketPaymentHistoryRepository;
import com.expo.payment.repository.TicketPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 토스 승인 실패 결과를 별도 트랜잭션으로 보존한다. */
@Service
@RequiredArgsConstructor
class TicketPaymentFailureService {

    private final TicketPaymentRepository ticketPaymentRepository;
    private final TicketPaymentHistoryRepository ticketPaymentHistoryRepository;

    @Transactional
    public void recordFailure(Long paymentId, String failureCode, String rawResponse) {
        TicketPayment payment =
                ticketPaymentRepository
                        .findById(paymentId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        if (payment.getStatus() == TicketPaymentStatus.DONE) {
            return;
        }
        TicketPaymentStatus statusBeforeAttempt = payment.getStatus();
        payment.fail(failureCode);
        ticketPaymentHistoryRepository.save(
                TicketPaymentHistory.record(
                        payment.getId(),
                        TicketPaymentEventType.FAIL,
                        statusBeforeAttempt,
                        TicketPaymentStatus.FAILED,
                        null,
                        null,
                        rawResponse));
    }
}
