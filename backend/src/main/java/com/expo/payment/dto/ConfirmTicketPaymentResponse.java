package com.expo.payment.dto;

import com.expo.payment.entity.TicketPaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** 티켓 결제 승인 결과. */
public record ConfirmTicketPaymentResponse(
        Long paymentId,
        String orderNumber,
        String paymentKey,
        String method,
        TicketPaymentStatus status,
        BigDecimal amount,
        Instant approvedAt) {}
