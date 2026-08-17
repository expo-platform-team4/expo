package com.expo.ticket.dto;

import com.expo.ticket.entity.TicketOrderStatus;
import java.math.BigDecimal;
import java.util.List;

public record GuestTicketSearchResponse(
        String orderNumber,
        TicketOrderStatus status,
        List<TicketOrderItemResponse> items,
        BigDecimal ticketSubtotalAmount,
        BigDecimal bookingFeeAmount,
        BigDecimal totalAmount) {}
