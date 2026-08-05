package com.expo.auth.dto;

import com.expo.auth.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일반 회원 로컬 회원가입 응답")
public record SignupResponse(
        @Schema(description = "사용자 ID") Long userId,
        @Schema(description = "이메일") String email,
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "역할") Role role) {}
