package com.expo.expo.entity;

/**
 * 박람회 개최 신청 처리 상태 — {@code expo_opening_requests.status} CHECK 제약과 값이 같다.
 *
 * <p>{@link com.expo.recruitment.entity.RecruitmentNoticeRequestStatus} 와 값 집합이 같다. 두 도메인이 "주최사가
 * 요청하고 관리자가 심사한다" 는 같은 모양을 공유하기 때문이다.
 */
public enum ExpoOpeningRequestStatus {
    /** 작성 중. 주최사만 보이고 수정할 수 있다. */
    DRAFT,
    /** 심사 요청됨. 이 시점부터 관리자 목록에 뜨고 주최사는 수정할 수 없다. */
    SUBMITTED,
    /** 관리자가 검토를 시작했다. */
    UNDER_REVIEW,
    /** 승인됨. 이때 {@code expos} 행이 만들어진다. */
    APPROVED,
    /** 반려됨. 사유가 함께 기록된다. */
    REJECTED,
    /** 주최사가 철회했다. */
    CANCELED
}
