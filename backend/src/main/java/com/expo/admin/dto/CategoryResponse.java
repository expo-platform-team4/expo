package com.expo.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 관리자 카테고리 응답 (E-API-014~017). */
@Schema(description = "카테고리")
public record CategoryResponse(
        @Schema(description = "카테고리 ID") Long id,
        @Schema(description = "상위 카테고리 ID") Long parentId,
        @Schema(description = "카테고리명") String name,
        @Schema(description = "슬러그") String slug,
        @Schema(description = "노출 순서") int sortOrder,
        @Schema(description = "활성 상태") boolean active) {}
