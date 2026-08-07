package com.expo.booth.entity;

/** 부스 배정·콘텐츠에 대한 운영상 변경 처리 종류. 신청 승인·반려 이력이 아니다. */
public enum BoothManagementActionType {
    ALLOCATION_CORRECTED,
    INFORMATION_UPDATED,
    CORRECTION_REQUESTED,
    CONTENT_HIDDEN,
    CONTENT_RESTORED
}
