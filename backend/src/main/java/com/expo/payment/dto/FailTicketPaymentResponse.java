package com.expo.payment.dto;

import com.expo.payment.entity.TicketPaymentStatus;
import com.expo.ticket.entity.TicketOrderStatus;

/** 티켓 결제 실패 처리 결과. */
public record FailTicketPaymentResponse(
        Long paymentId,
        String orderNumber,
        TicketPaymentStatus paymentStatus,
        TicketOrderStatus orderStatus) {}
