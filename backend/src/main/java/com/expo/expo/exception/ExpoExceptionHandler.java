package com.expo.expo.exception;

import com.expo.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 박람회 도메인 전용 예외 처리기.
 *
 * <p>{@link ExpoStateException} 은 도메인 상태 전이 규칙 위반(예: 승인 후 직접 수정 시도 — 희-EXPO-06, 본인 소유가
 * 아닌 신청 처리 시도)에 대응하며 409 CONFLICT 로 매핑한다. {@code BusinessException} 과 {@code @Valid} 검증
 * 실패는 모든 도메인이 공유하는 {@link com.expo.common.exception.GlobalExceptionHandler} 가 처리하므로 여기 두지
 * 않는다 (AuthExceptionHandler, BannerExceptionHandler 와 동일 컨벤션).
 */
@RestControllerAdvice
public class ExpoExceptionHandler {

    /** 박람회/개최 신청 상태 전이 규칙 위반 처리. */
    @ExceptionHandler(ExpoStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpoStateException(ExpoStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(ex.getMessage()));
    }
}
