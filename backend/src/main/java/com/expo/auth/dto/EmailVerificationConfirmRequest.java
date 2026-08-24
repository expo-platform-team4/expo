package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "이메일 본인인증 결과 확인 요청")
public record EmailVerificationConfirmRequest(
        @Schema(description = "인증 요청 ID (요청 API 응답값)", example = "1")
                @NotNull(message = "인증 요청 ID는 필수입니다.")
                Long verificationId,
        @Schema(description = "이메일로 받은 6자리 인증코드", example = "482913")
                @NotBlank(message = "인증코드는 필수입니다.")
                @Pattern(regexp = "^\\d{6}$", message = "인증코드는 6자리 숫자입니다.")
                String verificationCode) {}
