package com.expo.expo.domain;

/**
 * 박람회 심사/공개 상태 (테이블 정의서 No.12 status)
 * DRAFT → SUBMITTED → UNDER_REVIEW → PUBLISHED / REJECTED
 * PUBLISHED → CANCELLATION_REQUESTED → CANCELLED, 행사 종료 후 CLOSED
 */
public enum ExpoStatus {

    /** 임시저장 (희-EXPO-01) */
    DRAFT("임시저장"),

    /** 심사 요청됨 (희-EXPO-02) */
    SUBMITTED("심사요청"),

    /** 심사 중 */
    UNDER_REVIEW("심사중"),

    /** 반려 */
    REJECTED("반려"),

    /** 승인 = 자동 공개 (희-EXPO-09) */
    PUBLISHED("공개"),

    /** 취소 요청 */
    CANCELLATION_REQUESTED("취소요청"),

    /** 취소 완료 */
    CANCELLED("취소"),

    /** 종료 */
    CLOSED("종료");

    private final String description;

    ExpoStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 클라이언트가 직접 수정 가능한 상태인지 (희-EXPO-05 / 희-EXPO-06)
     * 승인(PUBLISHED) 전 상태에서만 직접 수정 가능
     */
    public boolean isEditableByClient() {
        return this == DRAFT || this == SUBMITTED || this == UNDER_REVIEW || this == REJECTED;
    }

    /** 심사 요청 가능한 상태인지 (임시저장/반려 상태에서 재요청 가능) */
    public boolean isSubmittable() {
        return this == DRAFT || this == REJECTED;
    }
}
