package com.expo.auth.exception;

/**
 * 인증 도메인에서 비즈니스 규칙이 깨졌을 때 Service 계층이 던지는 예외.
 *
 * <p>입력 형식 오류({@code @Valid})와 달리, <b>도메인 판단 결과</b>로 실패한 경우에 쓴다. 예: 이미 가입된 이메일, 중복 닉네임.
 *
 * <p>{@link AuthExceptionHandler}의 {@code @ExceptionHandler(BusinessException.class)}가 이 예외를 잡아
 * {@link ErrorCode}에 정의된 HTTP 상태 코드와 메시지로 {@link com.expo.auth.dto.AuthApiResponse}를 반환한다.
 *
 * <pre>{@code
 * // AuthService 예시
 * throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
 * }</pre>
 */
public class BusinessException extends RuntimeException {

  /** 이 예외에 대응하는 HTTP 상태·메시지 정의. */
  private final ErrorCode errorCode;

  /**
   * @param errorCode 발생한 비즈니스 오류 종류 (상태 코드·메시지 포함)
   */
  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  /** {@link AuthExceptionHandler}가 응답 HTTP 상태를 결정할 때 사용한다. */
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
