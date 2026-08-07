package com.expo.recruitment.entity;

/** 모집 마감 후 결제·배정 완료 기업을 집계한 결과 스냅샷의 상태. */
public enum RecruitmentResultStatus {
    GENERATED,
    DELIVERED,
    CONFIRMED,
    USED_FOR_EXPO,
    CANCELED
}
