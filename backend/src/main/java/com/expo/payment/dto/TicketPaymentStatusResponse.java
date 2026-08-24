package com.expo.payment.dto;

import com.expo.payment.entity.TicketPaymentStatus;
import com.expo.ticket.entity.TicketOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** 주문의 최신 티켓 결제 처리 상태. 결제 시작 전에는 결제 관련 필드가 비어 있을 수 있다. */
public record TicketPaymentStatusResponse(
        String orderNumber,
        TicketOrderStatus orderStatus,
        Long paymentId,
        TicketPaymentStatus paymentStatus,
        BigDecimal amount,
        Instant approvedAt,
        String failureCode) {}
