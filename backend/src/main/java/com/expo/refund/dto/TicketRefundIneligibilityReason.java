package com.expo.refund.dto;

/** 전체 주문 환불이 불가능한 이유. {@code null}이면 환불 가능하다. */
public enum TicketRefundIneligibilityReason {
    ORDER_NOT_PAID,
    EVENT_STARTS_WITHIN_THREE_DAYS,
    TICKET_ALREADY_CHECKED_IN
}
