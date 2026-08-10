package com.expo.ticket.dto;

import com.expo.ticket.entity.TicketProductStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TicketProductSearchResponse(
        Long ticketProductId,
        String name,
        String description,
        BigDecimal price,
        Instant salesStartAt,
        Instant salesEndAt,
        int totalQuantity,
        int availableQuantity,
        int maxQuantityPerOrder,
        TicketProductStatus status) {}
