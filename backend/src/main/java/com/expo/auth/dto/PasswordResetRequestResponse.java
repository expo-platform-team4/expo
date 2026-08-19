package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 비밀번호 재설정 요청 응답 (A-API-013). */
@Schema(description = "비밀번호 재설정 요청 결과")
public record PasswordResetRequestResponse(
        @Schema(description = "요청한 이메일") String email,
        @Schema(description = "재설정 토큰 만료 시각") Instant expiresAt,
        @Schema(description = "안내 메시지") String message) {}
