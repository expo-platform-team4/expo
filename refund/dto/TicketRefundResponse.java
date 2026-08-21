package com.expo.refund.dto;

import com.expo.refund.entity.TicketRefundStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** 티켓 주문 환불 요청 결과. */
public record TicketRefundResponse(
        Long refundId,
        Long ticketOrderId,
        Long ticketPaymentId,
        BigDecimal refundAmount,
        TicketRefundStatus status,
        Instant requestedAt) {}
