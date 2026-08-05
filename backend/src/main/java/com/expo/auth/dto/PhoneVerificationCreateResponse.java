package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "휴대폰 본인인증 요청 응답")
public record PhoneVerificationCreateResponse(
    @Schema(description = "인증 요청 ID", example = "1") Long verificationId,
    @Schema(description = "정규화된 휴대폰 번호", example = "01012345678") String phoneNumber,
    @Schema(description = "인증 만료 시각", example = "2026-08-05T12:47:00+09:00") Instant expiresAt,
    @Schema(description = "안내 메시지", example = "인증번호가 발송되었습니다.") String message) {}
