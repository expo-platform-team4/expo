package com.expo.refund.dto;

import java.math.BigDecimal;

/** 비회원 티켓 주문의 전체 환불 가능 여부와 예상 환불 금액. */
public record TicketRefundEligibilityResponse(
        boolean refundable,
        TicketRefundIneligibilityReason ineligibilityReason,
        BigDecimal expectedRefundAmount) {}
