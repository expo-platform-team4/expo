package com.expo.banner.exception;

import com.expo.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 배너 도메인 전용 예외 처리기.
 *
 * <p>{@link BannerStateException} 은 도메인 상태 전이 규칙 위반(예: 심사 대기 상태가 아닌 신청을 승인/반려하려는 경우)에
 * 대응하며 409 CONFLICT 로 매핑한다. {@code BusinessException} 과 {@code @Valid} 검증 실패는 모든 도메인이 공유하는
 * {@link com.expo.common.exception.GlobalExceptionHandler} 가 처리하므로 여기 두지 않는다
 * (AuthExceptionHandler 와 동일 컨벤션).
 *
 * <p>주의: {@code ExpoStateException} 은 아직 이런 전용 핸들러가 없어 잡히면 500 으로 응답된다. 지금까지는 상태
 * 위반이 실제로 발생하지 않아 드러나지 않았을 뿐이므로, 그쪽도 동일하게 추가하는 걸 권장한다.
 */
@RestControllerAdvice
public class BannerExceptionHandler {

    /** 배너 신청/노출 상태 전이 규칙 위반 처리. */
    @ExceptionHandler(BannerStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleBannerStateException(BannerStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(ex.getMessage()));
    }
}
