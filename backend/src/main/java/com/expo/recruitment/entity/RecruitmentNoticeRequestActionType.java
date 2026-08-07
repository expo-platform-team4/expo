package com.expo.recruitment.entity;

/** 모집공고 생성 요청 이력에 기록되는 처리 유형. */
public enum RecruitmentNoticeRequestActionType {
    SUBMIT,
    REVIEW_START,
    VENUE_ALLOW,
    VENUE_CANCEL,
    APPROVE,
    REJECT,
    NOTICE_CREATED
}
