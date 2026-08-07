package com.expo.expo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 박람회 심사 이력 응답. */
@Schema(description = "박람회 심사 이력")
public record ExpoReviewHistoryResponse(
        @Schema(description = "이력 ID") Long id,
        @Schema(description = "박람회 ID") Long expoId,
        @Schema(description = "심사한 관리자 ID") Long reviewerAdminId,
        @Schema(description = "처리 유형", example = "REJECT") String decision,
        @Schema(description = "반려/취소 사유") String reason,
        @Schema(description = "이전 상태") String fromStatus,
        @Schema(description = "이후 상태") String toStatus,
        @Schema(description = "처리 시각") Instant reviewedAt) {}
