package com.expo.auth.exception;

/** 휴대폰 번호 형식 오류. */
public class InvalidPhoneNumberException extends RuntimeException {

  public InvalidPhoneNumberException(String message) {
    super(message);
  }
}
