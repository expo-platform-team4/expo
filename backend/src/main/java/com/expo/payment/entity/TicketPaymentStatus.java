package com.expo.payment.entity;

/** 토스페이먼츠 티켓 결제 상태. 승인 성공 상태는 명세에 따라 DONE 을 사용한다. */
public enum TicketPaymentStatus {
    READY,
    IN_PROGRESS,
    DONE,
    FAILED,
    CANCELED
}
