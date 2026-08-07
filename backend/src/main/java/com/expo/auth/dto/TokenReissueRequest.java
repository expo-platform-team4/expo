package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Access Token 재발급 요청")
public record TokenReissueRequest(
        @Schema(
                        description = "로그인 시 발급받은 Refresh Token (원문)",
                        example = "550e8400-e29b-41d4-a716-446655440000")
                @NotBlank(message = "Refresh Token은 필수입니다.")
                String refreshToken) {}
