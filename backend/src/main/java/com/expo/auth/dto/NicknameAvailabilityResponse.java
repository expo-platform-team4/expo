package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 닉네임 사용 가능 여부 응답 (A-API-004).
 *
 * <p>{@code valid}(형식 통과)와 {@code duplicate}(DB 중복)를 분리해 프론트에서 상세 사유를 구분할 수 있게 한다. {@code
 * available} 은 두 조건이 모두 통과했을 때만 {@code true}.
 */
@Schema(description = "닉네임 사용 가능 여부 응답")
public record NicknameAvailabilityResponse(
    @Schema(description = "검사한 닉네임(trim 적용)", example = "expo_member") String nickname,
    @Schema(description = "닉네임 형식 통과 여부", example = "true") boolean valid,
    @Schema(description = "DB 에 이미 사용 중인 닉네임인지 여부", example = "false") boolean duplicate,
    @Schema(description = "회원가입에 사용 가능한지 여부", example = "true") boolean available,
    @Schema(description = "안내 메시지", example = "사용 가능한 닉네임입니다.") String message) {

  public static NicknameAvailabilityResponse available(String nickname) {
    return new NicknameAvailabilityResponse(nickname, true, false, true, "사용 가능한 닉네임입니다.");
  }

  public static NicknameAvailabilityResponse duplicate(String nickname) {
    return new NicknameAvailabilityResponse(nickname, true, true, false, "이미 사용 중인 닉네임입니다.");
  }
}
