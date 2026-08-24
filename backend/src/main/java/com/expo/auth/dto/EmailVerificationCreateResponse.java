package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "이메일 본인인증 요청 응답")
public record EmailVerificationCreateResponse(
        @Schema(description = "인증 요청 ID", example = "1") Long verificationId,
        @Schema(description = "정규화된 이메일", example = "member@espotic.com") String email,
        @Schema(description = "인증 만료 시각", example = "2026-08-21T12:47:00+09:00") Instant expiresAt,
        @Schema(description = "안내 메시지", example = "인증코드가 발송되었습니다.") String message) {}
