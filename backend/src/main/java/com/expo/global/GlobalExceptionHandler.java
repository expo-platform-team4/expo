package com.expo.global;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(
            CustomException customException) {
        ErrorCode errorCode = customException.getErrorCode();

        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
        MethodArgumentNotValidException methodArgumentNotValidException
    ) {
       ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        return ResponseEntity.status(errorCode.getStatus())
            .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
    }
}
