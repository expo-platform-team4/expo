package com.expo.refund.dto;

import jakarta.validation.constraints.Size;

/** 전체 티켓 주문 환불 요청. */
public record TicketRefundRequest(
        @Size(max = 1000, message = "환불 사유는 1,000자 이하여야 합니다.") String reason) {}
