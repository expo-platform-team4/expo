package com.expo.booth.entity;

/** 부스 결제 이력에 기록되는 이벤트 종류. */
public enum BoothPaymentEventType {
    REQUEST,
    APPROVE,
    FAIL,
    CANCEL
}
