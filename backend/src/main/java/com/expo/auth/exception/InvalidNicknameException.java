package com.expo.auth.exception;

/**
 * 닉네임 필수값·길이 오류 시 서비스 계층이 던지는 예외.
 *
 * <p>{@link AuthExceptionHandler} 에서 400 Bad Request 로 응답한다.
 */
public class InvalidNicknameException extends RuntimeException {

  public InvalidNicknameException(String message) {
    super(message);
  }
}
