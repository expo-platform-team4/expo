package com.expo.checkin.entity;

/** 발권된 개별 입장권의 상태. 체크인 취소·복구는 지원하지 않는다. */
public enum IssuedTicketStatus {
    ISSUED, // 발권 완료, 아직 미입장
    CHECKED_IN, // 현장 입장 처리됨
    CANCELED, // 주문 환불로 취소됨
    INVALIDATED // 박람회 취소 등으로 무효화됨
}
