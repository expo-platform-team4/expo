package com.expo.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 업로드된 파일 한 건.
 *
 * <p>{@code storageKey}·{@code bucketName} 은 담지 않는다 — 저장소 내부 구조라 밖에서 알 필요가 없고, 알면 버킷 구조를 추측하는
 * 단서가 된다. 파일을 받는 데 필요한 것은 {@code downloadUrl} 하나다.
 */
@Schema(description = "파일 메타데이터")
public record FileMetadataResponse(
        @Schema(description = "파일 ID") Long fileId,
        @Schema(description = "원본 파일명") String originalFilename,
        @Schema(description = "MIME 타입") String contentType,
        @Schema(description = "크기(Byte)") Long fileSize,
        @Schema(description = "공개 범위", example = "PUBLIC") String accessLevel,
        @Schema(description = "내려받기 경로", example = "/api/files/12/content") String downloadUrl) {}
