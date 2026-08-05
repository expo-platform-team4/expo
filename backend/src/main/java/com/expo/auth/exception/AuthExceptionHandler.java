package com.expo.auth.exception;

import com.expo.auth.controller.AuthController;
import com.expo.auth.dto.AuthApiResponse;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * {@link AuthController} 전용 예외 처리기.
 *
 * <p>Service에서 던진 {@link BusinessException}과, DTO {@code @Valid} 검증 실패를 {@link AuthApiResponse} 공통
 * 형식으로 변환해 클라이언트에 반환한다.
 */
@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

  /**
   * 비즈니스 규칙 위반 예외 처리.
   *
   * <p>{@link com.expo.auth.service.AuthService}에서 중복 이메일·닉네임 등으로 {@link BusinessException}을 던지면 이
   * 메서드가 호출된다. {@link ErrorCode}에 정의된 HTTP 상태(예: 409 CONFLICT)와 메시지를 응답한다.
   *
   * <p>응답 예: {@code { "success": false, "data": null, "message": "이미 사용 중인 이메일입니다." }}
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<AuthApiResponse<Void>> handleBusinessException(BusinessException ex) {
    ErrorCode errorCode = ex.getErrorCode();
    return ResponseEntity.status(errorCode.getStatus()).body(AuthApiResponse.fail(ex.getMessage()));
  }

  /**
   * 사업자등록번호 필수값·형식 오류 처리.
   *
   * <p>{@link com.expo.auth.service.BusinessNumberValidationService#normalize(String)} 에서 null·빈
   * 값·정규식 불일치를 감지하면 {@link InvalidBusinessNumberException} 을 던진다. 항상 400 Bad Request 로 응답한다.
   */
  @ExceptionHandler(InvalidBusinessNumberException.class)
  public ResponseEntity<AuthApiResponse<Void>> handleInvalidBusinessNumber(
      InvalidBusinessNumberException ex) {
    return ResponseEntity.badRequest().body(AuthApiResponse.fail(ex.getMessage()));
  }

  /** 이메일 필수값·형식 오류 처리 (A-API-003). */
  @ExceptionHandler(InvalidEmailException.class)
  public ResponseEntity<AuthApiResponse<Void>> handleInvalidEmail(InvalidEmailException ex) {
    return ResponseEntity.badRequest().body(AuthApiResponse.fail(ex.getMessage()));
  }

  /**
   * 동시 가입 등으로 DB unique 제약이 위반된 경우 처리.
   *
   * <p>Service 내부에서도 변환하지만, User 저장 직후 ClientProfile 저장 전에 다른 요청이 먼저 가입하는 경우를 대비한다.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<AuthApiResponse<Void>> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause().getMessage();
    if (message != null) {
      String lower = message.toLowerCase();
      if (lower.contains("business_number")) {
        return ResponseEntity.status(ErrorCode.DUPLICATE_BUSINESS_NUMBER.getStatus())
            .body(AuthApiResponse.fail(ErrorCode.DUPLICATE_BUSINESS_NUMBER.getMessage()));
      }
      if (lower.contains("email")) {
        return ResponseEntity.status(ErrorCode.DUPLICATE_EMAIL.getStatus())
            .body(AuthApiResponse.fail(ErrorCode.DUPLICATE_EMAIL.getMessage()));
      }
      if (lower.contains("nickname")) {
        return ResponseEntity.status(ErrorCode.DUPLICATE_NICKNAME.getStatus())
            .body(AuthApiResponse.fail(ErrorCode.DUPLICATE_NICKNAME.getMessage()));
      }
    }
    return ResponseEntity.badRequest()
        .body(AuthApiResponse.fail(ErrorCode.INVALID_INPUT.getMessage()));
  }

  /**
   * 요청 DTO 검증 실패 처리.
   *
   * <p>{@link com.expo.auth.dto.SignupRequest}의 {@code @NotBlank}, {@code @Email}, {@code @Size} 등이
   * 실패하면 Spring이 {@link MethodArgumentNotValidException}을 던진다. 필드별 기본 메시지를 쉼표로 이어 하나의 문자열로 만들어 400
   * Bad Request로 반환한다.
   *
   * <p>응답 예: {@code { "success": false, "data": null, "message": "이메일은 필수입니다., 비밀번호는 8자 이상..." }}
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<AuthApiResponse<Void>> handleValidationException(
      MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(AuthApiResponse.fail(message));
  }
}
