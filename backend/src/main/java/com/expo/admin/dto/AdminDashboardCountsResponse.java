package com.expo.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 대시보드 처리 대기 건수 집계 응답 ({@code v_admin_dashboard_counts} 뷰 기반).
 *
 * <p>항상 단일 행을 반환한다.
 */
@Schema(description = "관리자 대시보드 처리 대기 건수")
public record AdminDashboardCountsResponse(
        @Schema(description = "박람회 개최 심사 대기 건수") long pendingExpoReviewCount,
        @Schema(description = "배너 심사 대기 건수") long pendingBannerReviewCount,
        @Schema(description = "모집공고 생성 요청 대기 건수") long pendingNoticeRequestCount,
        @Schema(description = "장소 충돌 검토 대기 건수") long venueConflictCount,
        @Schema(description = "미확인 참여 신청 건수") long uncheckedApplicationCount,
        @Schema(description = "박람회 수정 요청 대기 건수") long pendingChangeRequestCount,
        @Schema(description = "박람회 취소 요청 대기 건수") long pendingCancellationRequestCount,
        @Schema(description = "정산 대기 건수") long settlementWaitingCount) {}
