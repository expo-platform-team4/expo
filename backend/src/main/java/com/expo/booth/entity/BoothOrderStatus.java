package com.expo.booth.entity;

/** 참여 신청 과정에서 선택한 단일 부스 상품 주문의 상태. */
public enum BoothOrderStatus {
    PENDING_PAYMENT,
    PAYMENT_COMPLETED,
    FAILED,
    CANCELED,
    EXPIRED
}
