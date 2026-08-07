package com.expo.participation.entity;

/** 참여 기업 신청서의 처리 상태. 관리자 승인·반려 모델은 사용하지 않는다. */
public enum ParticipationApplicationStatus {
    DRAFT,
    PAYMENT_PENDING,
    SUBMITTED,
    PAYMENT_FAILED,
    CANCELED
}
