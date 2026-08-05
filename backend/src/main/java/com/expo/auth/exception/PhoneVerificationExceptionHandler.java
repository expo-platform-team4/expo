package com.expo.auth.exception;

import com.expo.auth.controller.PhoneVerificationController;
import com.expo.auth.dto.AuthApiResponse;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * {@link PhoneVerificationController} 전용 예외 처리기.
 *
 * <p>Service의 {@link InvalidPhoneNumberException}과 요청 DTO {@code @Valid} 실패를 {@link AuthApiResponse}
 * 공통 형식으로 변환해 400 Bad Request 로 응답한다.
 */
@RestControllerAdvice(assignableTypes = PhoneVerificationController.class)
public class PhoneVerificationExceptionHandler {

  /**
   * 휴대폰 번호 필수값·형식 오류 처리.
   *
   * <p>{@link com.expo.auth.service.PhoneVerificationService#normalize(String)} 에서 null·빈 값·010 형식 불일치 시
   * 던진다. 응답 예: {@code { "success": false, "data": null, "message": "휴대폰 번호 형식이 올바르지 않습니다." }}
   */
  @ExceptionHandler(InvalidPhoneNumberException.class)
  public ResponseEntity<AuthApiResponse<Void>> handleInvalidPhoneNumber(
      InvalidPhoneNumberException ex) {
    return ResponseEntity.badRequest().body(AuthApiResponse.fail(ex.getMessage()));
  }

  /**
   * 요청 DTO 검증 실패 처리 ({@link com.expo.auth.dto.PhoneVerificationCreateRequest} 등).
   *
   * <p>{@code @NotBlank}, {@code @Pattern} 등이 실패하면 Spring이 {@link MethodArgumentNotValidException}을 던진다.
   * 필드별 메시지를 쉼표로 이어 하나의 문자열로 반환한다.
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