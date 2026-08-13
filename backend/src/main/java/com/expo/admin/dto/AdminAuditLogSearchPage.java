package com.expo.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 관리자 운영 이력 검색 결과 한 페이지 (E-API-018).
 *
 * @param totalCount 전체 건수. 화면이 페이지 수를 계산한다
 */
@Schema(description = "관리자 운영 이력 페이지")
public record AdminAuditLogSearchPage(
        @Schema(description = "전체 건수", example = "1024") long totalCount,
        @Schema(description = "요청한 페이지 번호 (0부터)", example = "0") int page,
        @Schema(description = "페이지 크기", example = "20") int size,
        @Schema(description = "이력 목록") List<AdminAuditLogResponse> items) {}
