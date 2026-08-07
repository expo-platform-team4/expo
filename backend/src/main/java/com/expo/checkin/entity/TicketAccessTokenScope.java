package com.expo.checkin.entity;

/** 접근 토큰이 열어 주는 범위. */
public enum TicketAccessTokenScope {
    ORDER_VIEW, // 주문 1건 전체 조회
    QR_VIEW // 특정 발권 티켓의 QR 조회
}
