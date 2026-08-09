package com.expo.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 비즈니스 오류 종류와 그에 대응하는 HTTP 상태·메시지. 모든 도메인이 공유한다.
 *
 * <p>도메인별로 묶어서 적는다. 새 도메인을 추가할 때는 빈 줄로 구분해 아래에 붙인다.
 */
public enum ErrorCode {

    // --- 인증 · 회원가입 ---
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    DUPLICATE_BUSINESS_NUMBER(HttpStatus.CONFLICT, "이미 가입된 사업자등록번호입니다."),

    // --- 로그인 · 토큰 ---
    INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    WITHDRAWN_ACCOUNT(HttpStatus.FORBIDDEN, "탈퇴한 계정입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),

    // --- 휴대폰 본인인증 ---
    PHONE_VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 요청입니다."),
    PHONE_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다."),
    PHONE_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다."),

    // --- 티켓 ---
    INVALID_SALES_PERIOD(HttpStatus.BAD_REQUEST, "판매 시작 시간은 종료 시간보다 빨라야 합니다."),

    // --- 관리자 · 카테고리 ---
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."),
    DUPLICATE_CATEGORY_NAME(HttpStatus.CONFLICT, "이미 사용 중인 카테고리명입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
