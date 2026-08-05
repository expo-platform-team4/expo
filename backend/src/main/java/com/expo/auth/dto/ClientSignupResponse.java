package com.expo.auth.dto;

import com.expo.auth.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "클라이언트 로컬 회원가입 응답")
public record ClientSignupResponse(
        @Schema(description = "사용자 ID", example = "1") Long userId,
        @Schema(description = "역할", example = "CLIENT") Role role,
        @Schema(description = "이메일", example = "client@test.com") String email,
        @Schema(description = "회사명", example = "테스트 주식회사") String companyName,
        @Schema(description = "완료 메시지", example = "클라이언트 회원가입이 완료되었습니다.") String message) {}
