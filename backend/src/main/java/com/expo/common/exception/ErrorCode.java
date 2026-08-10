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

    // --- 박람회 ---
    EXPO_NOT_FOUND(HttpStatus.BAD_REQUEST, "박람회를 찾을 수 없습니다."),
    NOT_EXPO_HOST(HttpStatus.BAD_REQUEST, "해당 박람회의 주최자가 아닙니다."),

    // --- 관리자 · 카테고리 ---
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."),
    DUPLICATE_CATEGORY_NAME(HttpStatus.CONFLICT, "이미 사용 중인 카테고리명입니다."),

    // --- 가상 장소 ---
    DUPLICATE_VIRTUAL_VENUE_NAME(HttpStatus.CONFLICT, "이미 등록된 장소명입니다."),
    VIRTUAL_VENUE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 가상 장소입니다."),
    DUPLICATE_VENUE_HALL_CODE(HttpStatus.CONFLICT, "이미 등록된 홀 코드입니다."),
    VENUE_HALL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 홀입니다."),
    DUPLICATE_VENUE_ZONE_CODE(HttpStatus.CONFLICT, "이미 등록된 구역 코드입니다."),
    VENUE_ZONE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 구역입니다."),
    RECRUITMENT_NOTICE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 모집공고 생성 요청입니다."),
    VENUE_RESERVATION_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "사용 종료 일시는 시작 일시보다 늦어야 합니다."),
    VENUE_RESERVATION_PERIOD_CONFLICT(HttpStatus.CONFLICT, "같은 장소·기간에 이미 확정된 예약이 있습니다."),
    VENUE_RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 장소 예약입니다."),
    VENUE_RESERVATION_ALREADY_RELEASED(HttpStatus.CONFLICT, "이미 해제되었거나 취소된 예약입니다."),
    VENUE_HALL_ZONE_MISMATCH(HttpStatus.BAD_REQUEST, "홀·구역이 지정한 장소·홀 소속이 아닙니다."),
    RECRUITMENT_NOTICE_REQUEST_NOT_ALLOWED(
            HttpStatus.CONFLICT, "장소 충돌 판정에서 승인(ALLOWED)된 요청만 장소를 예약할 수 있습니다."),

    // --- 부스 ---
    DUPLICATE_BOOTH_TEMPLATE_SHAPE_CODE(HttpStatus.CONFLICT, "이미 등록된 형태 코드입니다."),
    BOOTH_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 부스 템플릿입니다."),
    DUPLICATE_BOOTH_NUMBER(HttpStatus.CONFLICT, "이미 등록된 부스 번호입니다."),
    BOOTH_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 부스입니다."),
    DUPLICATE_BOOTH_PRODUCT(HttpStatus.CONFLICT, "이미 해당 공고에 등록된 부스 상품입니다."),
    BOOTH_SALES_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "판매 종료 일시는 시작 일시보다 늦어야 합니다."),
    BOOTH_PRODUCT_NOT_EDITABLE(HttpStatus.CONFLICT, "예약·판매 완료된 부스 상품의 상태는 관리자가 직접 바꿀 수 없습니다."),

    // --- 모집공고 ---
    DUPLICATE_RECRUITMENT_NOTICE_REQUEST(HttpStatus.CONFLICT, "이미 공고가 생성된 요청입니다."),
    RECRUITMENT_NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 기업 모집 공고입니다."),
    RECRUITMENT_NOTICE_NOT_EDITABLE(HttpStatus.CONFLICT, "초안 상태에서만 공고를 수정할 수 있습니다."),
    RECRUITMENT_NOTICE_NOT_PUBLISHABLE(HttpStatus.CONFLICT, "초안 상태에서만 공고를 게시할 수 있습니다."),
    RECRUITMENT_NOTICE_NOT_CLOSABLE(HttpStatus.CONFLICT, "게시 중인 공고만 마감할 수 있습니다."),
    VENUE_DECISION_INVALID(HttpStatus.BAD_REQUEST, "장소 결정은 ALLOWED 또는 CANCELED 만 가능합니다."),
    VENUE_DECISION_ALREADY_MADE(HttpStatus.CONFLICT, "이미 장소 결정이 완료된 요청입니다."),
    APPLICATION_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "신청 종료 일시는 시작 일시보다 늦어야 합니다."),
    EVENT_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "행사 종료 일시는 시작 일시보다 늦어야 합니다."),
    RECRUITMENT_NOTICE_CREATION_NOT_ALLOWED(
            HttpStatus.CONFLICT, "장소 충돌 판정에서 승인(ALLOWED)된 요청만 공고를 생성할 수 있습니다."),

    // --- 참여 신청 ---
    BOOTH_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 부스 상품입니다."),
    PARTICIPATION_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 참여 신청서입니다."),
    RECRUITMENT_NOTICE_NOT_OPEN(HttpStatus.CONFLICT, "게시 중인 모집공고에만 참여 신청할 수 있습니다.");

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
