package com.expo.payment.dto;

import com.expo.payment.entity.TicketPaymentEventType;
import com.expo.payment.entity.TicketPaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** 관리자 티켓 결제 승인·실패·취소 이력. */
public record AdminTicketPaymentHistoryResponse(
        Long historyId,
        Long orderId,
        String orderNumber,
        Long paymentId,
        TicketPaymentEventType eventType,
        TicketPaymentStatus fromStatus,
        TicketPaymentStatus toStatus,
        BigDecimal amount,
        String pgTransactionKey,
        Instant occurredAt) {}
