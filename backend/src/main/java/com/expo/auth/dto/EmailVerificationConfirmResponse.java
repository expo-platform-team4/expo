package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "이메일 본인인증 결과 확인 응답")
public record EmailVerificationConfirmResponse(
        @Schema(description = "인증 요청 ID", example = "1") Long verificationId,
        @Schema(description = "정규화된 이메일", example = "member@espotic.com") String email,
        @Schema(description = "인증 완료 시각", example = "2026-08-21T13:30:00+09:00") Instant verifiedAt,
        @Schema(
                        description = "회원가입 API에서 사용하는 인증 완료 토큰 (원문, 1회 발급)",
                        example = "7f3c2a1b-9d4e-5f6a-8b7c-1d2e3f4a5b6c")
                String signupVerificationToken,
        @Schema(description = "안내 메시지", example = "이메일 인증이 완료되었습니다.") String message) {}
