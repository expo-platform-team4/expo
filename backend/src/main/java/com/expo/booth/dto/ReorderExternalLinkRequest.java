package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 부스 콘텐츠 외부 링크 노출 순서 변경 요청. */
@Schema(description = "부스 콘텐츠 외부 링크 노출 순서 변경 요청")
public record ReorderExternalLinkRequest(
        @Schema(description = "노출 순서") @NotNull(message = "노출 순서는 필수입니다.") Integer sortOrder) {}
