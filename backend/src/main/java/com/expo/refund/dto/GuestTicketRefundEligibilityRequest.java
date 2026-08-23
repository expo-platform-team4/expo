package com.expo.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 비회원 주문 환불 가능 여부를 조회하기 위한 주문 인증 값. */
public record GuestTicketRefundEligibilityRequest(
        @NotBlank String orderNumber,
        @NotBlank @Size(max = 20) String phoneNumber,
        @NotBlank @Size(max = 100) String password) {}
