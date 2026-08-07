package com.expo.banner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 배너 신청 심사 이력 응답. */
@Schema(description = "배너 신청 심사 이력")
public record BannerReviewHistoryResponse(
        @Schema(description = "이력 ID") Long id,
        @Schema(description = "배너 신청 ID") Long bannerApplicationId,
        @Schema(description = "심사한 관리자 ID") Long reviewerAdminId,
        @Schema(description = "처리 유형", example = "REJECT") String decision,
        @Schema(description = "반려/취소 사유") String reason,
        @Schema(description = "이전 상태") String fromStatus,
        @Schema(description = "이후 상태") String toStatus,
        @Schema(description = "처리 시각") Instant reviewedAt) {}
