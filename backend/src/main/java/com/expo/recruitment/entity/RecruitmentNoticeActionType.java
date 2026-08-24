package com.expo.recruitment.entity;

/** 모집공고 작성·게시·수정·마감·취소 이력에 기록되는 처리 유형. */
public enum RecruitmentNoticeActionType {
    CREATE,
    PUBLISH,
    ACTIVATE,
    UPDATE,
    CLOSE,
    CANCEL,
    ARCHIVE
}
