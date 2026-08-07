package com.expo.checkin.entity;

/** 접근 토큰의 상태. */
public enum TicketAccessTokenStatus {
    ACTIVE, // 사용 가능
    EXPIRED, // 유효기간 만료
    REVOKED // 관리자·시스템이 폐기
}
