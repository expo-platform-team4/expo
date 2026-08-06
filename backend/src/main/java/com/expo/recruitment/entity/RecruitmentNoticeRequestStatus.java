package com.expo.recruitment.entity;

/** 주최 클라이언트의 모집공고 생성 요청 처리 상태. */
public enum RecruitmentNoticeRequestStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    CANCELED
}
