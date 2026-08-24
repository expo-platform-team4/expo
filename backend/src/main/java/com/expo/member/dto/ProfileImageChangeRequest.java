package com.expo.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 프로필 이미지 등록·교체 요청 (A-API-017).
 *
 * <p>이미지 바이트 자체는 담지 않는다 — 먼저 {@code POST /api/files}(purpose=PROFILE_IMAGE)로 올린 뒤, 그
 * 응답의 {@code fileId} 를 여기로 보내 내 계정에 연결한다.
 */
@Schema(description = "프로필 이미지 변경 요청")
public record ProfileImageChangeRequest(
        @Schema(description = "미리 업로드한 파일의 ID (POST /api/files 응답)", example = "42")
                @NotNull(message = "fileId는 필수입니다.")
                Long fileId) {}
