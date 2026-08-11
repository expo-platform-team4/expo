package com.expo.recruitment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 모집 결과 항목 응답. */
@Schema(description = "모집 결과 항목")
public record RecruitmentResultItemResponse(
        @Schema(description = "항목 ID") Long id,
        @Schema(description = "참여 신청서 ID") Long applicationId,
        @Schema(description = "참여 기업 클라이언트 사용자 ID") Long clientUserId,
        @Schema(description = "부스 확정 배정 ID") Long boothAllocationId,
        @Schema(description = "부스 결제 금액") BigDecimal boothAmount,
        @Schema(description = "생성 일시") LocalDateTime createdAt) {}
