package com.expo.expo.dto;

import com.expo.expo.entity.ExpoFilePurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 이미 업로드된 파일을 박람회 자료로 연결한다. */
@Schema(description = "박람회 자료 연결 요청")
public record AttachExpoFileRequest(
        @Schema(description = "업로드된 파일 ID") @NotNull(message = "파일 ID는 필수입니다.") Long fileId,
        @Schema(description = "자료 종류") @NotNull(message = "자료 종류는 필수입니다.")
                ExpoFilePurpose filePurpose,
        @Schema(description = "표시할 제목. 비우면 원본 파일명을 쓴다.")
                @Size(max = 150, message = "제목은 150자를 넘을 수 없습니다.")
                String title) {}
