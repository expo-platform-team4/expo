package com.expo.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 비회원 전체 티켓 주문 환불 요청. 주문 인증 값은 URL이 아닌 요청 body로 받는다. */
public record GuestTicketRefundRequest(
        @NotBlank String orderNumber,
        @NotBlank @Size(max = 20) String phoneNumber,
        @NotBlank @Size(max = 100) String password,
        @Size(max = 1000, message = "환불 사유는 1,000자 이하여야 합니다.") String reason) {}
