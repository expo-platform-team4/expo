package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 비밀번호 재설정 요청 (A-API-013). */
@Schema(description = "비밀번호 재설정 요청")
public record PasswordResetRequest(
        @Schema(description = "가입한 이메일", example = "member@espotic.com")
                @NotBlank(message = "이메일은 필수입니다.")
                @Email(message = "올바른 이메일 형식이 아닙니다.")
                String email) {}
