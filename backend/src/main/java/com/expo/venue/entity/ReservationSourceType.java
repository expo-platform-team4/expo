package com.expo.venue.entity;

/** 장소 예약이 어느 경로로 생성됐는지. 모집공고 경로인지 일반 박람회 등록 경로인지 구분한다. */
public enum ReservationSourceType {
    RECRUITMENT_NOTICE,
    EXPO_DIRECT
}
