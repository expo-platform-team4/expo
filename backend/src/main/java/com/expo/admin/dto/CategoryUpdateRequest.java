package com.expo.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** 카테고리명·노출순서·활성상태 수정 요청 (E-API-016). 전달된 필드만 변경한다. */
@Schema(description = "카테고리 수정 요청")
public record CategoryUpdateRequest(
        @Size(max = 100, message = "카테고리명은 100자 이하여야 합니다.") @Schema(description = "카테고리명")
                String name,
        @Schema(description = "노출 순서") Integer sortOrder,
        @Schema(description = "활성 상태") Boolean active) {}
