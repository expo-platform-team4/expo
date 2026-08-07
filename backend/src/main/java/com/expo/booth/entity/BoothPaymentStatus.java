package com.expo.booth.entity;

/** 토스페이먼츠 부스 결제 상태. 승인 완료 후에는 환불을 지원하지 않는다. */
public enum BoothPaymentStatus {
    READY,
    IN_PROGRESS,
    APPROVED,
    CANCELED,
    FAILED
}
