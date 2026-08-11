package com.expo.booth.dto;

import com.expo.booth.entity.BoothManagementActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 부스 배정·콘텐츠 운영 변경 이력 응답. */
@Schema(description = "부스 운영 변경 이력")
public record BoothManagementHistoryResponse(
        @Schema(description = "이력 ID") Long id,
        @Schema(description = "처리 종류") BoothManagementActionType actionType,
        @Schema(description = "사유") String reason,
        @Schema(description = "처리 관리자 ID") Long processedByAdminId,
        @Schema(description = "생성 일시") LocalDateTime createdAt) {}
