package com.expo.participation.entity;

/** 참여 신청 운영 확인·보완 요청 이력에 기록되는 처리 유형. 승인·반려 이력이 아니다. */
public enum ApplicationOperationActionType {
    CHECKED,
    CORRECTION_REQUESTED,
    CORRECTION_COMPLETED,
    MEMO_UPDATED
}
