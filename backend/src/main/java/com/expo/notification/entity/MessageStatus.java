package com.expo.notification.entity;

/** 발송 시도 한 건의 외부 시스템 처리 상태. */
public enum MessageStatus {
    REQUESTED, // 대행사에 요청함
    SENT, // 대행사가 접수함
    DELIVERED, // 수신자에게 도달함
    FAILED // 실패
}
