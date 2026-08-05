package com.expo.auth.exception;

/**
 * 이메일 필수값·형식 오류 시 서비스 계층이 던지는 예외.
 *
 * <p>{@link AuthExceptionHandler} 에서 400 Bad Request 로 응답한다.
 */
public class InvalidEmailException extends RuntimeException {

  //이메일 형식이 잘못됐을 때 던지는 커스텀 예외의 생성자
  public InvalidEmailException(String message) {
    super(message);
  }
}
