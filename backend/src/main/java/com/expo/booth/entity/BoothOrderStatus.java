package com.expo.booth.entity;

/** 부스 상품 주문의 처리 상태. */
public enum BoothOrderStatus {
    PENDING_PAYMENT,
    PAYMENT_COMPLETED,
    FAILED,
    CANCELED,
    EXPIRED
}
