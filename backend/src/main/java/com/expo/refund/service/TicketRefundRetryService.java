package com.expo.refund.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 실패한 티켓 환불을 같은 실행 흐름으로 재처리한다. */
@Service
@RequiredArgsConstructor
public class TicketRefundRetryService {

    private final TicketRefundRetryValidationService ticketRefundRetryValidationService;
    private final TicketRefundExecutionService ticketRefundExecutionService;

    public void retry(Long refundId) {
        ticketRefundRetryValidationService.validateRetryable(refundId);
        ticketRefundExecutionService.execute(refundId);
    }
}
