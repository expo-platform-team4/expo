package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 API 응답")
public record AuthApiResponse<T>(
    @Schema(description = "성공 여부") boolean success,
    @Schema(description = "응답 데이터") T data,
    @Schema(description = "오류 메시지") String message) {

  public static <T> AuthApiResponse<T> ok(T data) {
    return new AuthApiResponse<>(true, data, null);
  }

  public static AuthApiResponse<Void> fail(String message) {
    return new AuthApiResponse<>(false, null, message);
  }
}
