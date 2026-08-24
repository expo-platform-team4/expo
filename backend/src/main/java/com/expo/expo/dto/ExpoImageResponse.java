package com.expo.expo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 박람회에 붙은 이미지 한 장. */
@Schema(description = "박람회 이미지")
public record ExpoImageResponse(
        @Schema(description = "연결 ID") Long id,
        @Schema(description = "파일 ID") Long fileId,
        @Schema(description = "이미지 종류", example = "THUMBNAIL") String imageType,
        @Schema(description = "대체 텍스트") String altText,
        @Schema(description = "노출 순서") Integer sortOrder,
        @Schema(description = "이미지 경로", example = "/api/files/12/content") String url) {}
