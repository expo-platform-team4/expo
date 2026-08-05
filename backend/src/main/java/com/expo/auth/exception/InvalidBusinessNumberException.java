package com.expo.auth.exception;

/**
 * 사업자등록번호 필수값·형식 오류 시 서비스 계층이 던지는 예외.
 *
 * <p>{@link BusinessException}(ErrorCode 기반)과 달리, 메시지가 상황(빈 값 / 형식 오류 / 자릿수 오류)마다 달라져야 하므로 별도 클래스로
 * 둔다.
 *
 * <p>{@link AuthExceptionHandler} 에서 400 Bad Request 로 응답한다.
 */
public class InvalidBusinessNumberException extends RuntimeException {

    public InvalidBusinessNumberException(String message) {
        super(message);
    }
}
