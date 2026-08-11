package com.expo.participation.dto;

import com.expo.participation.entity.ApplicationOperationActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 참여 신청 운영 확인·보완 요청 이력 응답. */
@Schema(description = "참여 신청 운영 이력")
public record ApplicationOperationHistoryResponse(
        @Schema(description = "이력 ID") Long id,
        @Schema(description = "신청서 ID") Long applicationId,
        @Schema(description = "처리 유형") ApplicationOperationActionType actionType,
        @Schema(description = "메모·사유") String message,
        @Schema(description = "처리한 관리자 ID") Long processedByAdminId,
        @Schema(description = "생성 일시") LocalDateTime createdAt) {}
