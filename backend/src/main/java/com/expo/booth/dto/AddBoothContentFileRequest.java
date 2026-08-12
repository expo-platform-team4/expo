package com.expo.booth.dto;

import com.expo.booth.entity.BoothContentFileType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 부스 콘텐츠 첨부 파일 등록 요청. */
@Schema(description = "부스 콘텐츠 첨부 파일 등록 요청")
public record AddBoothContentFileRequest(
        @Schema(description = "원본 파일 ID") @NotNull(message = "파일 ID는 필수입니다.") Long fileId,
        @Schema(description = "파일 종류") @NotNull(message = "파일 종류는 필수입니다.")
                BoothContentFileType fileType,
        @Schema(description = "제목") String title,
        @Schema(description = "노출 순서") Integer sortOrder) {}
