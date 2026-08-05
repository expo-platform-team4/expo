package com.expo.auth.exception;

import com.expo.auth.controller.LoginController;
import com.expo.auth.dto.AuthApiResponse;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** {@link LoginController} 전용 예외 처리기. */
@RestControllerAdvice(assignableTypes = LoginController.class)
public class LoginExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<AuthApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(AuthApiResponse.fail(ex.getMessage()));
    }

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
