package com.expo.ticket.dto;

import java.math.BigDecimal;

public record TicketOrderItemResponse(
        Long ticketProductId,
        String ticketName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal itemSubtotalAmount) {}
