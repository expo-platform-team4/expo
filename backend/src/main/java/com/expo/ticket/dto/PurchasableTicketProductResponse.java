package com.expo.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PurchasableTicketProductResponse(
        Long ticketProductId,
        String name,
        String description,
        BigDecimal price,
        int availableQuantity,
        int maxQuantityPerOrder,
        Instant salesStartAt,
        Instant salesEndAt) {}
