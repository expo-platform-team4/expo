package com.expo.refund.converter;

import com.expo.refund.dto.TicketRefundResponse;
import com.expo.refund.entity.TicketRefund;
import org.springframework.stereotype.Component;

/** 티켓 환불 엔티티를 API 응답으로 변환한다. */
@Component
public class TicketRefundConverter {

    public TicketRefundResponse toResponse(TicketRefund refund) {
        return new TicketRefundResponse(
                refund.getId(),
                refund.getTicketOrderId(),
                refund.getTicketPaymentId(),
                refund.getRefundAmount(),
                refund.getStatus(),
                refund.getRequestedAt());
    }
}
