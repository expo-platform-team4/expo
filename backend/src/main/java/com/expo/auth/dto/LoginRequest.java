package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "이메일·비밀번호 로그인 요청")
public record LoginRequest(
        @Schema(description = "이메일(아이디)", example = "member@espotic.com")
                @NotBlank(message = "이메일은 필수입니다.")
                @Email(message = "올바른 이메일 형식이 아닙니다.")
                @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
                String email,
        @Schema(description = "비밀번호", example = "Test1234!") @NotBlank(message = "비밀번호는 필수입니다.")
                String password) {}
