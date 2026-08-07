package com.expo.notification.entity;

/** 알림 작업 단위의 발송 상태. */
public enum NotificationStatus {
    PENDING, // 발송 대기
    SENT, // 발송 성공
    FAILED, // 재시도까지 소진하고 실패
    RETRYING, // 실패 후 재시도 대기
    CANCELED // 발송하지 않기로 함 (예: 수신 번호 없음)
}
