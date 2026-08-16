package com.expo.ticket.entity;

/** 주문의 결제 진행 상태. DB ticket_orders.status 제약값과 일치한다. */
public enum TicketOrderStatus {
    PENDING,
    PAID,
    CANCELED,
    PAYMENT_FAILED,
    EXPIRED
}
