package com.expo.booth.dto;

import com.expo.booth.entity.ExternalLinkType;
import io.swagger.v3.oas.annotations.media.Schema;

/** 부스 콘텐츠 외부 링크 응답. */
@Schema(description = "부스 콘텐츠 외부 링크")
public record ExternalLinkResponse(
        @Schema(description = "링크 ID") Long id,
        @Schema(description = "링크 종류") ExternalLinkType linkType,
        @Schema(description = "표시 라벨") String label,
        @Schema(description = "URL") String url,
        @Schema(description = "노출 순서") Integer sortOrder) {}
