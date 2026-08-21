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
    GUEST_ORDER_LOOKUP_LOCKED(HttpStatus.BAD_REQUEST, "10분 재시도 하십시오."),

    // --- 로그인 · 토큰 ---
    INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    WITHDRAWN_ACCOUNT(HttpStatus.FORBIDDEN, "탈퇴한 계정입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),

    // --- 휴대폰 본인인증 ---
    PHONE_VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 요청입니다."),
    PHONE_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다."),
    PHONE_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다."),

    // --- 비밀번호 재설정 ---
    PASSWORD_RESET_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "유효하지 않은 재설정 토큰입니다."),
    PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "재설정 토큰이 만료되었습니다."),

    // --- 티켓 ---
    INVALID_SALES_PERIOD(HttpStatus.BAD_REQUEST, "판매 시작 시간은 종료 시간보다 빨라야 합니다."),
    TICKET_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 티켓 입니다."),
    TICKET_TOTAL_QUANTITY_TOO_LOW(HttpStatus.BAD_REQUEST, "총 티켓 수량은 현재 예약 및 판매 수량보다 적을 수 없습니다"),
    TICKET_PRODUCT_UPDATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "판매 전 상태의 티켓만 수정할 수 있습니다"),
    TICKET_INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다"),
    DUPLICATE_TICKET_PRODUCT_IN_ORDER(HttpStatus.BAD_REQUEST, "동일한 티켓 상품을 중복 주문할 수 없습니다"),
    TICKET_PRODUCT_NOT_ON_SALE(HttpStatus.BAD_REQUEST, "판매 중인 티켓만 주문할 수 있습니다."),
    TICKET_PRODUCT_NOT_ON_SALE_PERIOD(HttpStatus.BAD_REQUEST, "티켓 판매 기간이 아닙니다."),
    TICKET_QUANTITY_EXCEEDS_LIMIT(HttpStatus.BAD_REQUEST, "티켓 상품별 최대 주문 수량을 초과했습니다."),
    TICKET_TOTAL_QUANTITY_EXCEEDED(HttpStatus.BAD_REQUEST, "티켓 최대 주문 수량을 초과 했습니다."),
    NOT_FOUND_ORDER_NUMBER(HttpStatus.BAD_REQUEST, "주문 번호를 확인할 수 없습니다"),
    GUEST_ORDER_LOOKUP_FAILED(HttpStatus.BAD_REQUEST, "주문 정보가 일치하지 않습니다."),

    // --- 박람회 ---
    EXPO_NOT_FOUND(HttpStatus.BAD_REQUEST, "박람회를 찾을 수 없습니다."),
    NOT_EXPO_HOST(HttpStatus.BAD_REQUEST, "해당 박람회의 주최자가 아닙니다."),

    // --- 관리자 · 카테고리 ---
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."),
    DUPLICATE_CATEGORY_NAME(HttpStatus.CONFLICT, "이미 사용 중인 카테고리명입니다."),

    // --- 관리자 · 계정 조회 ---
    ADMIN_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."),

    // --- 가상 장소 ---
    DUPLICATE_VIRTUAL_VENUE_NAME(HttpStatus.CONFLICT, "이미 등록된 장소명입니다."),
    VIRTUAL_VENUE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 가상 장소입니다."),
    VIRTUAL_VENUE_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "장소는 1개(킨텍스)까지만 등록할 수 있습니다."),
    DUPLICATE_VENUE_HALL_CODE(HttpStatus.CONFLICT, "이미 등록된 홀 코드입니다."),
    VENUE_HALL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 홀입니다."),
    VENUE_HALL_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "한 장소에는 전시장(홀)을 2개까지만 등록할 수 있습니다."),
    DUPLICATE_VENUE_ZONE_CODE(HttpStatus.CONFLICT, "이미 등록된 구역 코드입니다."),
    VENUE_ZONE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 구역입니다."),
    VENUE_ZONE_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "한 홀에는 구역을 5개까지만 등록할 수 있습니다."),
    RECRUITMENT_NOTICE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 모집공고 생성 요청입니다."),
    VENUE_RESERVATION_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "사용 종료 일시는 시작 일시보다 늦어야 합니다."),
    VENUE_RESERVATION_PERIOD_CONFLICT(HttpStatus.CONFLICT, "같은 장소·기간에 이미 확정된 예약이 있습니다."),
    VENUE_RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 장소 예약입니다."),
    VENUE_RESERVATION_ALREADY_RELEASED(HttpStatus.CONFLICT, "이미 해제되었거나 취소된 예약입니다."),
    VENUE_HALL_ZONE_MISMATCH(HttpStatus.BAD_REQUEST, "홀·구역이 지정한 장소·홀 소속이 아닙니다."),
    RECRUITMENT_NOTICE_REQUEST_NOT_ALLOWED(
            HttpStatus.CONFLICT, "장소 충돌 판정에서 승인(ALLOWED)된 요청만 장소를 예약할 수 있습니다."),
    RECRUITMENT_NOTICE_REQUEST_ZONES_EMPTY(HttpStatus.CONFLICT, "이 요청에 고른 구역이 없어 장소를 예약할 수 없습니다."),

    // --- 부스 ---
    DUPLICATE_BOOTH_TEMPLATE_SHAPE_CODE(HttpStatus.CONFLICT, "이미 등록된 형태 코드입니다."),
    BOOTH_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 부스 템플릿입니다."),
    DUPLICATE_BOOTH_NUMBER(HttpStatus.CONFLICT, "이미 등록된 부스 번호입니다."),
    BOOTH_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 부스입니다."),
    DUPLICATE_BOOTH_PRODUCT(HttpStatus.CONFLICT, "이미 해당 공고에 등록된 부스 상품입니다."),
    BOOTH_IN_USE_BY_OTHER_NOTICE(HttpStatus.CONFLICT, "이미 다른 모집공고에서 사용 중인 부스입니다."),
    BOOTH_SALES_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "판매 종료 일시는 시작 일시보다 늦어야 합니다."),
    BOOTH_PRODUCT_NOT_EDITABLE(HttpStatus.CONFLICT, "예약·판매 완료된 부스 상품의 상태는 관리자가 직접 바꿀 수 없습니다."),
    BOOTH_ORDER_NOT_ALLOWED(HttpStatus.CONFLICT, "초안 상태의 신청서만 부스 상품을 주문할 수 있습니다."),
    BOOTH_PRODUCT_NOT_SELECTED(HttpStatus.BAD_REQUEST, "신청서에 선택된 부스 상품이 없습니다."),
    BOOTH_PRODUCT_NOT_AVAILABLE(HttpStatus.CONFLICT, "구매 가능한 상태의 부스 상품이 아닙니다."),
    BOOTH_PRODUCT_SALES_NOT_OPEN(HttpStatus.CONFLICT, "지금은 부스 상품 판매 기간이 아닙니다."),
    BOOTH_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다."),
    BOOTH_ORDER_NOT_CANCELABLE(HttpStatus.CONFLICT, "결제 대기 상태의 주문만 취소할 수 있습니다."),

    // --- 결제 ---
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 결제입니다."),
    PAYMENT_ORDER_NOT_PENDING(HttpStatus.CONFLICT, "결제 대기 상태의 주문이 아닙니다."),
    PAYMENT_ORDER_EXPIRED(HttpStatus.CONFLICT, "주문이 만료되었습니다."),
    PAYMENT_ALREADY_APPROVED(HttpStatus.CONFLICT, "이미 승인된 결제입니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액이 주문 금액과 일치하지 않습니다."),
    PAYMENT_APPROVAL_FAILED(HttpStatus.BAD_GATEWAY, "결제 승인에 실패했습니다."),
    BOOTH_ALLOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 부스 배정입니다."),
    BOOTH_ALLOCATION_NOT_CANCELABLE(HttpStatus.CONFLICT, "이미 취소된 배정입니다."),
    BOOTH_ALLOCATION_NOT_ASSIGNED(HttpStatus.CONFLICT, "확정 배정 상태에서만 부스 콘텐츠를 작성할 수 있습니다."),
    PAYMENT_RESERVATION_NOT_FOUND(HttpStatus.CONFLICT, "결제 가능한 재고 예약을 찾을 수 없습니다."),

    // --- 부스 콘텐츠 ---
    BOOTH_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 부스 콘텐츠입니다."),
    DUPLICATE_BOOTH_CONTENT(HttpStatus.CONFLICT, "이미 해당 배정에 등록된 부스 콘텐츠입니다."),
    BOOTH_CONTENT_NOT_EDITABLE(HttpStatus.CONFLICT, "초안 또는 보완 요청 상태의 콘텐츠만 수정할 수 있습니다."),
    BOOTH_CONTENT_NOT_SUBMITTABLE(HttpStatus.CONFLICT, "초안 또는 보완 요청 상태의 콘텐츠만 검수 요청할 수 있습니다."),
    BOOTH_CONTENT_NOT_APPROVABLE(HttpStatus.CONFLICT, "검수 요청 상태의 콘텐츠만 승인(공개)할 수 있습니다."),
    BOOTH_CONTENT_NOT_CORRECTION_REQUESTABLE(
            HttpStatus.CONFLICT, "검수 요청 또는 공개 상태의 콘텐츠만 보완을 요청할 수 있습니다."),
    BOOTH_CONTENT_NOT_HIDABLE(HttpStatus.CONFLICT, "공개되었거나 보완 요청 상태의 콘텐츠만 숨길 수 있습니다."),
    BOOTH_CONTENT_NOT_RESTORABLE(HttpStatus.CONFLICT, "숨김 상태의 콘텐츠만 숨김 해제할 수 있습니다."),
    BOOTH_CONTENT_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 부스 콘텐츠 첨부 파일입니다."),
    DUPLICATE_BOOTH_CONTENT_FILE(HttpStatus.CONFLICT, "이미 등록된 첨부 파일입니다."),
    EXTERNAL_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 외부 링크입니다."),

    // --- 모집공고 ---
    DUPLICATE_RECRUITMENT_NOTICE_REQUEST(HttpStatus.CONFLICT, "이미 공고가 생성된 요청입니다."),
    RECRUITMENT_NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 기업 모집 공고입니다."),
    RECRUITMENT_NOTICE_NOT_EDITABLE(HttpStatus.CONFLICT, "초안 상태에서만 공고를 수정할 수 있습니다."),
    RECRUITMENT_NOTICE_NOT_PUBLISHABLE(HttpStatus.CONFLICT, "초안 상태에서만 공고를 게시할 수 있습니다."),
    RECRUITMENT_NOTICE_NOT_CLOSABLE(HttpStatus.CONFLICT, "게시 중인 공고만 마감할 수 있습니다."),
    RECRUITMENT_NOTICE_NOT_CANCELABLE(HttpStatus.CONFLICT, "이미 마감되었거나 취소된 공고는 취소할 수 없습니다."),
    VENUE_DECISION_INVALID(HttpStatus.BAD_REQUEST, "장소 결정은 ALLOWED 또는 CANCELED 만 가능합니다."),
    VENUE_DECISION_ALREADY_MADE(HttpStatus.CONFLICT, "이미 장소 결정이 완료된 요청입니다."),
    APPLICATION_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "신청 종료 일시는 시작 일시보다 늦어야 합니다."),
    EVENT_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "행사 종료 일시는 시작 일시보다 늦어야 합니다."),
    RECRUITMENT_NOTICE_CREATION_NOT_ALLOWED(
            HttpStatus.CONFLICT, "장소 충돌 판정에서 승인(ALLOWED)된 요청만 공고를 생성할 수 있습니다."),

    // --- 모집 결과 ---
    RECRUITMENT_NOTICE_NOT_CLOSED(HttpStatus.CONFLICT, "마감된 공고만 결과를 생성할 수 있습니다."),
    DUPLICATE_RECRUITMENT_RESULT(HttpStatus.CONFLICT, "이미 결과가 생성된 공고입니다."),
    RECRUITMENT_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 모집 결과입니다."),
    RECRUITMENT_RESULT_NOT_CANCELABLE(HttpStatus.CONFLICT, "이미 확정되어 박람회 구성에 반영된 결과는 취소할 수 없습니다."),

    // --- 참여 신청 ---
    BOOTH_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 부스 상품입니다."),
    PARTICIPATION_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 참여 신청서입니다."),
    RECRUITMENT_NOTICE_NOT_OPEN(HttpStatus.CONFLICT, "게시 중인 모집공고에만 참여 신청할 수 있습니다."),
    DUPLICATE_PARTICIPATION_APPLICATION(HttpStatus.CONFLICT, "이미 해당 공고에 유효한 참여 신청서가 있습니다."),
    CORRECTION_NOT_REQUESTED(HttpStatus.CONFLICT, "보완 요청이 없는 신청서는 보완 완료 처리할 수 없습니다."),

    // --- 발권 · QR ---
    TICKET_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다."),
    TICKET_ORDER_NOT_PAID(HttpStatus.CONFLICT, "결제가 완료된 주문만 발권할 수 있습니다."),
    TICKET_ORDER_HAS_NO_ITEM(HttpStatus.CONFLICT, "발권할 항목이 없는 주문입니다."),
    TICKET_ALREADY_ISSUED(HttpStatus.CONFLICT, "이미 발권된 주문입니다."),

    // --- 티켓 조회 링크 ---
    // 셋을 나눈 이유는 받는 사람이 할 수 있는 일이 다르기 때문이다.
    // 만료는 재발급을 안내할 수 있지만, 폐기는 안내하면 안 된다.
    TICKET_ACCESS_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "유효하지 않은 링크입니다."),
    TICKET_ACCESS_TOKEN_EXPIRED(HttpStatus.GONE, "링크 유효기간이 지났습니다."),
    TICKET_ACCESS_TOKEN_REVOKED(HttpStatus.FORBIDDEN, "사용할 수 없는 링크입니다."),

    // --- 알림 재발송 ---
    // 넷을 나눈 이유는 관리자가 다음에 할 일이 다르기 때문이다.
    // 못 보내는 이유가 상태 탓인지, 번호 탓인지, 템플릿 탓인지에 따라 조치가 갈린다.
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),
    NOTIFICATION_NOT_RETRYABLE(HttpStatus.CONFLICT, "발송에 실패한 알림만 재발송할 수 있습니다."),
    NOTIFICATION_RECIPIENT_UNREACHABLE(HttpStatus.CONFLICT, "수신 가능한 휴대폰 번호가 없어 재발송할 수 없습니다."),
    NOTIFICATION_TEMPLATE_NOT_REBUILDABLE(HttpStatus.CONFLICT, "본문을 다시 만들 수 없는 알림입니다."),

    // --- 정산 ---
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 정산입니다."),
    SETTLEMENT_NOT_RECALCULABLE(HttpStatus.CONFLICT, "확정된 정산은 다시 계산할 수 없습니다."),
    SETTLEMENT_NOT_CONFIRMABLE(HttpStatus.CONFLICT, "계산이 끝난 정산만 확정할 수 있습니다."),
    SETTLEMENT_NOT_REMITTABLE(HttpStatus.CONFLICT, "확정된 정산만 송금 결과를 기록할 수 있습니다."),
    INVALID_REMITTANCE_STATUS(HttpStatus.BAD_REQUEST, "송금 상태 값이 올바르지 않습니다."),

    // --- 클라이언트 마이페이지 ---
    CLIENT_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "클라이언트 프로필을 찾을 수 없습니다."),

    // --- 회원 마이페이지 ---
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    MEMBER_WITHDRAWAL_PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");

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
