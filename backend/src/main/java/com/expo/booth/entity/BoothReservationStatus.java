package com.expo.booth.entity;

/** 결제 진행 중 임시 확보된 부스 상품 예약의 상태. */
public enum BoothReservationStatus {
    ACTIVE,
    CONFIRMED,
    EXPIRED,
    RELEASED
}
