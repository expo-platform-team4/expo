package com.expo.auth.exception;

import com.expo.common.exception.ErrorCode;
import com.expo.common.response.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * auth 도메인 전용 예외 처리기.
 *
 * <p>여기에는 <b>auth 에서만 발생하는 예외</b>만 둔다. {@code BusinessException} 과 {@code @Valid} 검증 실패는 모든 도메인이
 * 같은 방식으로 처리하므로 {@link com.expo.common.exception.GlobalExceptionHandler} 로 올렸다.
 *
 * <p>이전에는 컨트롤러별로 {@code assignableTypes} 를 지정한 핸들러가 셋이었고 셋 다 같은 코드를 복사해 갖고 있었다. 공통 부분을 걷어내고 나니
 * 컨트롤러를 구분할 이유가 없어져 하나로 합쳤다.
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    /** 사업자등록번호 필수값·형식 오류 (A-API-005). */
    @ExceptionHandler(InvalidBusinessNumberException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidBusinessNumber(
            InvalidBusinessNumberException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }

    /** 이메일 필수값·형식 오류 (A-API-003). */
    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidEmail(InvalidEmailException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }

    /** 닉네임 필수값·길이 오류 (A-API-004). */
    @ExceptionHandler(InvalidNicknameException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidNickname(InvalidNicknameException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }

    /** 휴대폰 번호 필수값·형식 오류 (A-API-006). */
    @ExceptionHandler(InvalidPhoneNumberException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPhoneNumber(
            InvalidPhoneNumberException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }

    /**
     * 동시 가입 등으로 DB unique 제약이 위반된 경우.
     *
     * <p>Service 내부에서도 중복을 검사하지만, User 저장 직후 ClientProfile 저장 전에 다른 요청이 먼저 가입하는 경우를 대비한다. 어느 컬럼이
     * 걸렸는지는 DB 메시지로만 알 수 있어 문자열을 본다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message != null) {
            String lower = message.toLowerCase();
            if (lower.contains("business_number")) {
                return toResponse(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
            }
            if (lower.contains("email")) {
                return toResponse(ErrorCode.DUPLICATE_EMAIL);
            }
            if (lower.contains("nickname")) {
                return toResponse(ErrorCode.DUPLICATE_NICKNAME);
            }
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.getMessage()));
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getMessage()));
    }
}
