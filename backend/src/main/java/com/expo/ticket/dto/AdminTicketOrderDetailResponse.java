package com.expo.ticket.dto;

import com.expo.ticket.entity.TicketOrderStatus;
import com.expo.ticket.entity.TicketOrdererType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** 관리자 티켓 주문 상세. */
public record AdminTicketOrderDetailResponse(
        Long orderId,
        String orderNumber,
        TicketOrdererType ordererType,
        Long memberUserId,
        TicketOrderStatus status,
        BigDecimal ticketSubtotalAmount,
        BigDecimal bookingFeeAmount,
        BigDecimal totalAmount,
        Instant paidAt,
        Instant canceledAt,
        Instant createdAt,
        List<TicketOrderItemResponse> items) {}
