package com.expo.ticket.dto;

import com.expo.ticket.entity.TicketOrderStatus;
import com.expo.ticket.entity.TicketOrdererType;
import java.math.BigDecimal;
import java.time.Instant;

/** 관리자 티켓 주문 목록 행. */
public record AdminTicketOrderResponse(
        Long orderId,
        String orderNumber,
        TicketOrdererType ordererType,
        Long memberUserId,
        TicketOrderStatus status,
        int totalQuantity,
        BigDecimal totalAmount,
        Instant createdAt) {}
