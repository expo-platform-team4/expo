package com.expo.recruitment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 모집 공고와 신청 완료 수, 확정 배정 수 ({@code v_client_dashboard_recruitment} 뷰 기반). */
@Schema(description = "클라이언트 마이페이지 - 모집공고 현황")
public record ClientDashboardRecruitmentResponse(
        @Schema(description = "주최 클라이언트 ID") Long hostClientId,
        @Schema(description = "모집공고 ID") Long recruitmentNoticeId,
        @Schema(description = "공고 제목") String title,
        @Schema(description = "공고 상태") String status,
        @Schema(description = "신청 시작 시각") Instant applicationStartAt,
        @Schema(description = "신청 종료 시각") Instant applicationEndAt,
        @Schema(description = "게시 시각") Instant publishedAt,
        @Schema(description = "마감 시각") Instant closedAt,
        @Schema(description = "제출된 신청 수") long submittedApplicationCount,
        @Schema(description = "확정 배정 수") long confirmedAllocationCount) {}
