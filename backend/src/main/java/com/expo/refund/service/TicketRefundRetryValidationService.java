package com.expo.refund.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.refund.entity.TicketRefund;
import com.expo.refund.entity.TicketRefundStatus;
import com.expo.refund.repository.TicketRefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 환불 재처리의 짧은 잠금 검증 트랜잭션. */
@Service
@RequiredArgsConstructor
class TicketRefundRetryValidationService {

    private final TicketRefundRepository ticketRefundRepository;

    @Transactional
    public void validateRetryable(Long refundId) {
        TicketRefund refund =
                ticketRefundRepository
                        .findByIdForUpdate(refundId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));
        if (refund.getStatus() != TicketRefundStatus.FAILED) {
            throw new BusinessException(ErrorCode.REFUND_NOT_RETRYABLE);
        }
    }
}
