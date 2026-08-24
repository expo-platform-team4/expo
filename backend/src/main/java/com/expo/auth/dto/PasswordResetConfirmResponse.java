package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 비밀번호 재설정 확인 응답 (A-API-014). */
@Schema(description = "비밀번호 재설정 결과")
public record PasswordResetConfirmResponse(@Schema(description = "안내 메시지") String message) {}
