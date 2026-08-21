package com.expo.payment.service;

import com.expo.payment.dto.ConfirmTicketPaymentResponse;

/** 토스 호출 전 검증에서 확보한 티켓 결제 식별자. */
record TicketPaymentConfirmationTarget(
        Long ticketOrderId,
        Long ticketPaymentId,
        ConfirmTicketPaymentResponse alreadyApprovedResponse) {}
