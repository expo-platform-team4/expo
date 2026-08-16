package com.expo.ticket.dto;

import com.expo.ticket.entity.TicketOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TicketOrderResponse(
        Long ticketOrderId,
        String orderNumber,
        TicketOrderStatus status,
        List<TicketOrderItemResponse> items,
        BigDecimal ticketSubtotalAmount,
        BigDecimal bookingFeeAmount,
        BigDecimal totalAmount,
        Instant expiresAt,
        Instant createdAt) {}
