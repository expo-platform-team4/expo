package com.expo.common.exception;

import com.expo.common.response.ApiResponse;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 컨트롤러가 공유하는 예외 처리기.
 *
 * <p>여기 있는 둘은 도메인과 무관하게 어디서나 같은 방식으로 처리된다. 이전에는 컨트롤러마다 {@code @RestControllerAdvice} 를 두고 같은 코드를
 * 세 번 복사해 두었다.
 *
 * <ul>
 *   <li>{@link BusinessException} — 도메인 규칙 위반. {@link ErrorCode} 의 상태·메시지로 응답
 *   <li>{@link MethodArgumentNotValidException} — 요청 DTO {@code @Valid} 실패
 * </ul>
 *
 * <p>도메인 전용 예외는 그 도메인의 {@code exception} 패키지에 자기 핸들러를 둔다. 예: {@link
 * com.expo.auth.exception.AuthExceptionHandler}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 규칙 위반 처리.
     *
     * <p>응답 예: {@code { "success": false, "message": "이미 사용 중인 이메일입니다." }}
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.fail(ex.getMessage()));
    }

    /** {@code @Version} 낙관적 락 충돌 처리. 트랜잭션 밖에서 잡아야 UnexpectedRollbackException을 피한다. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex) {
        ErrorCode errorCode = ErrorCode.RESOURCE_BUSY;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getMessage()));
    }

    /**
     * 요청 DTO 검증 실패 처리.
     *
     * <p>필드별 기본 메시지를 쉼표로 이어 하나의 문자열로 만든다.
     *
     * <p>응답 예: {@code { "success": false, "message": "이메일은 필수입니다., 비밀번호는 8자 이상..." }}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }
}
