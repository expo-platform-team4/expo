package com.expo.booth.entity;

/** 결제 진행 중 임시 확보한 부스 예약의 상태. */
public enum BoothReservationStatus {
    ACTIVE,
    CONFIRMED,
    EXPIRED,
    RELEASED
}
