package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 이메일 사용 가능 여부 응답 (A-API-003).
 *
 * <p>{@code valid}(형식 통과)와 {@code duplicate}(DB 중복)를 분리해 프론트에서 상세 사유를 구분할 수 있게 한다. {@code
 * available} 은 두 조건이 모두 통과했을 때만 {@code true}.
 */
@Schema(description = "이메일 사용 가능 여부 응답")
public record EmailAvailabilityResponse(
    @Schema(description = "검사한 이메일(trim 적용)", example = "member@espotic.com") String email,
    @Schema(description = "이메일 형식 통과 여부", example = "true") boolean valid,
    @Schema(description = "DB 에 이미 가입된 이메일인지 여부", example = "false") boolean duplicate,
    @Schema(description = "회원가입에 사용 가능한지 여부", example = "true") boolean available,
    @Schema(description = "안내 메시지", example = "사용 가능한 이메일입니다.") String message) {

  public static EmailAvailabilityResponse available(String email) {
    return new EmailAvailabilityResponse(email, true, false, true, "사용 가능한 이메일입니다.");
  }

  public static EmailAvailabilityResponse duplicate(String email) {
    return new EmailAvailabilityResponse(email, true, true, false, "이미 사용 중인 이메일입니다.");
  }
}
