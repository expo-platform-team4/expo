package com.expo.global;

import org.springframework.http.HttpStatus;

// 커스텀 에러코드 작성
/*
   도메인별로 구분해서 작성해 주세요
   ex) // Ticket
          TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "존재하지 않는 티켓 입니다.")
*/
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),

    // Ticket
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "존재하지 않는 티켓입니다."),
    INVALID_SALES_PERIOD(HttpStatus.BAD_REQUEST, "T002", "판매 시작 시간은 종료 시간보다 빨라야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
