package com.expo.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 심사 대기 목록 통합 응답 ({@code v_admin_pending_reviews} 뷰 기반).
 *
 * <p>박람회 개최 신청 / 배너 신청 / 모집공고 생성 요청 세 원천을 {@code reviewTargetType} 으로 구분해 한 목록으로 반환한다.
 */
@Schema(description = "관리자 심사 대기 항목")
public record AdminPendingReviewResponse(
        @Schema(description = "심사 대상 종류", example = "EXPO_OPENING") String reviewTargetType,
        @Schema(description = "대상 ID") Long targetId,
        @Schema(description = "제목") String title,
        @Schema(description = "요청한 클라이언트 ID") Long requesterClientId,
        @Schema(description = "처리 상태") String status,
        @Schema(description = "제출 시각") LocalDateTime submittedAt,
        @Schema(description = "생성 시각") LocalDateTime createdAt) {}
