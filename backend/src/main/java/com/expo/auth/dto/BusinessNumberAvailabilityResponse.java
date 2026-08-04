package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사업자등록번호 사용 가능 여부 응답.
 *
 * <p>{@code valid}(형식·테스트 번호 통과)와 {@code duplicate}(DB 중복)를 분리해 프론트에서 상세 사유를 구분할 수 있게 한다. {@code
 * available} 은 두 조건이 모두 통과했을 때만 {@code true}.
 */
@Schema(description = "사업자등록번호 사용 가능 여부 응답")
public record BusinessNumberAvailabilityResponse(
    @Schema(description = "정규화된 사업자등록번호(숫자 10자리)", example = "1234567890") String businessNumber,
    @Schema(description = "형식·테스트 번호 통과 여부", example = "true") boolean valid,
    @Schema(description = "DB 에 이미 가입된 번호인지 여부", example = "false") boolean duplicate,
    @Schema(description = "회원가입에 사용 가능한지 여부", example = "true") boolean available,
    @Schema(description = "안내 메시지", example = "사용 가능한 사업자등록번호입니다.") String message) {

  public static BusinessNumberAvailabilityResponse available(String normalizedBusinessNumber) {
    return new BusinessNumberAvailabilityResponse(
        normalizedBusinessNumber, true, false, true, "사용 가능한 사업자등록번호입니다.");
  }

  public static BusinessNumberAvailabilityResponse notTestNumber(String normalizedBusinessNumber) {
    return new BusinessNumberAvailabilityResponse(
        normalizedBusinessNumber, false, false, false, "테스트용으로 등록되지 않은 사업자등록번호입니다.");
  }

  public static BusinessNumberAvailabilityResponse duplicate(String normalizedBusinessNumber) {
    return new BusinessNumberAvailabilityResponse(
        normalizedBusinessNumber, true, true, false, "이미 가입된 사업자등록번호입니다.");
  }
}
