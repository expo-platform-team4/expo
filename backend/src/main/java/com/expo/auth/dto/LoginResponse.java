package com.expo.auth.dto;

import com.expo.auth.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "이메일·비밀번호 로그인 응답")
public record LoginResponse(
        @Schema(description = "JWT Access Token") String accessToken,
        @Schema(description = "Access Token 유효 시간(분)", example = "30")
                int accessTokenExpiresInMinutes,
        @Schema(description = "Refresh Token (원문)") String refreshToken,
        @Schema(description = "Refresh Token 만료 시각") Instant refreshTokenExpiresAt,
        @Schema(description = "사용자 ID", example = "1") Long userId,
        @Schema(description = "이메일", example = "member@espotic.com") String email,
        @Schema(description = "닉네임", example = "expo_member") String nickname,
        @Schema(description = "역할", example = "MEMBER") Role role) {}
