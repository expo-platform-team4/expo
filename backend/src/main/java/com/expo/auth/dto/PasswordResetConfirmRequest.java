package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 비밀번호 재설정 확인·새 비밀번호 저장 요청 (A-API-014). */
@Schema(description = "비밀번호 재설정 확인 요청")
public record PasswordResetConfirmRequest(
        @Schema(description = "재설정 요청 API가 발급한 토큰 원문") @NotBlank(message = "재설정 토큰은 필수입니다.")
                String resetToken,
        @Schema(description = "새 비밀번호 (영문·숫자·특수문자 조합 8자 이상)", example = "NewTest1234!")
                @NotBlank(message = "새 비밀번호는 필수입니다.")
                @Pattern(
                        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$",
                        message = "비밀번호는 영문·숫자·특수문자를 포함해 8자 이상이어야 합니다.")
                String newPassword,
        @Schema(description = "새 비밀번호 확인", example = "NewTest1234!")
                @NotBlank(message = "새 비밀번호 확인은 필수입니다.")
                String newPasswordConfirm) {}
