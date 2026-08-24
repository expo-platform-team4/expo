package com.expo.refund.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.refund.entity.TicketRefund;
import com.expo.refund.entity.TicketRefundStatus;
import com.expo.refund.repository.TicketRefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 토스 취소 실패 결과를 별도 트랜잭션으로 보존한다. */
@Service
@RequiredArgsConstructor
class TicketRefundExecutionFailureService {

    private final TicketRefundRepository ticketRefundRepository;

    @Transactional
    public void recordFailure(Long refundId, String failureCode) {
        TicketRefund refund =
                ticketRefundRepository
                        .findByIdForUpdate(refundId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));
        if (refund.getStatus() != TicketRefundStatus.COMPLETED) {
            refund.fail(failureCode);
        }
    }
}
