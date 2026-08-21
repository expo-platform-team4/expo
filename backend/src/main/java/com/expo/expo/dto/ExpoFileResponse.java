package com.expo.expo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 박람회에 붙은 자료 한 건. */
@Schema(description = "박람회 자료")
public record ExpoFileResponse(
        @Schema(description = "연결 ID") Long id,
        @Schema(description = "파일 ID") Long fileId,
        @Schema(description = "자료 종류", example = "LEAFLET") String filePurpose,
        @Schema(description = "표시 제목") String title,
        @Schema(description = "노출 순서") Integer sortOrder,
        @Schema(description = "내려받기 경로") String url) {}
