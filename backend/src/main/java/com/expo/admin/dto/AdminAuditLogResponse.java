package com.expo.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 관리자 운영 이력 항목 ({@code v_admin_audit_logs} 뷰 기반, E-API-018). */
@Schema(description = "관리자 운영 이력")
public record AdminAuditLogResponse(
        @Schema(description = "이력 종류", example = "EXPO_REVIEW") String logType,
        @Schema(description = "원본 이력 테이블 PK") Long logId,
        @Schema(description = "대상 ID") Long targetId,
        @Schema(description = "처리 종류", example = "APPROVE") String action,
        @Schema(description = "변경 전 상태") String fromStatus,
        @Schema(description = "변경 후 상태") String toStatus,
        @Schema(description = "사유·오류 내용") String reason,
        @Schema(description = "처리 관리자 ID (결제 이벤트는 PG 응답이라 NULL)") Long processorAdminId,
        @Schema(description = "처리 시각") Instant occurredAt) {}
