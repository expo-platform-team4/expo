package com.expo.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 박람회 카테고리 등록 요청 (E-API-015). */
@Schema(description = "카테고리 등록 요청")
public record CategoryCreateRequest(
        @Schema(description = "상위 카테고리 ID (없으면 최상위)") Long parentId,
        @NotBlank(message = "카테고리명은 필수입니다.")
                @Size(max = 100, message = "카테고리명은 100자 이하여야 합니다.")
                @Schema(description = "카테고리명")
                String name,
        @Schema(description = "노출 순서 (기본값 0)") Integer sortOrder) {}
