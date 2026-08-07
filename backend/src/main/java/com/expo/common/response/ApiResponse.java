package com.expo.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 모든 도메인이 공유하는 API 응답 봉투.
 *
 * <p>도메인마다 응답 형식을 따로 만들면 클라이언트가 엔드포인트별로 다른 파싱을 해야 한다. 실패 응답은 {@link
 * com.expo.common.exception.GlobalExceptionHandler} 가 이 형식으로 만든다.
 *
 * <pre>{@code
 * 성공  { "success": true,  "data": { ... }, "message": null }
 * 실패  { "success": false, "data": null,    "message": "이미 사용 중인 이메일입니다." }
 * }</pre>
 *
 * <p>{@code data} 가 {@code null} 인 필드는 응답 JSON 에서 빠진다 ({@code
 * spring.jackson.default-property-inclusion: non_null}).
 */
@Schema(description = "API 공통 응답")
public record ApiResponse<T>(
        @Schema(description = "성공 여부") boolean success,
        @Schema(description = "응답 데이터") T data,
        @Schema(description = "오류 메시지") String message) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
