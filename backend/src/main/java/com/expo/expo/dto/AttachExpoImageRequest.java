package com.expo.expo.dto;

import com.expo.expo.entity.ExpoImageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 이미 업로드된 파일을 박람회 이미지로 연결한다. 파일 자체는 {@code POST /api/files} 로 먼저 올린다. */
@Schema(description = "박람회 이미지 연결 요청")
public record AttachExpoImageRequest(
        @Schema(description = "업로드된 파일 ID") @NotNull(message = "파일 ID는 필수입니다.") Long fileId,
        @Schema(description = "이미지 종류") @NotNull(message = "이미지 종류는 필수입니다.")
                ExpoImageType imageType,
        @Schema(description = "대체 텍스트") @Size(max = 255, message = "대체 텍스트는 255자를 넘을 수 없습니다.")
                String altText) {}
