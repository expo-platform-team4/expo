package com.expo.booth.dto;

import com.expo.booth.entity.BoothContentFileType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 부스 콘텐츠 첨부 파일 응답. */
@Schema(description = "부스 콘텐츠 첨부 파일")
public record BoothContentFileResponse(
        @Schema(description = "첨부 파일 ID") Long id,
        @Schema(description = "원본 파일 ID") Long fileId,
        @Schema(description = "파일 종류") BoothContentFileType fileType,
        @Schema(description = "제목") String title,
        @Schema(description = "노출 순서") Integer sortOrder,
        @Schema(description = "생성 일시") LocalDateTime createdAt) {}
