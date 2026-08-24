package com.expo.payment.entity;

/** 티켓 결제 이력에 기록되는 이벤트 종류. */
public enum TicketPaymentEventType {
    REQUEST,
    APPROVE,
    FAIL,
    CANCEL
}
