package com.expo.recruitment.entity;

/** 허용된 장소 요청을 기준으로 관리자가 작성·게시하는 기업 모집공고의 상태. */
public enum RecruitmentNoticeStatus {
    DRAFT,
    SCHEDULED,
    OPEN,
    CLOSED,
    CANCELED,
    ARCHIVED
}
