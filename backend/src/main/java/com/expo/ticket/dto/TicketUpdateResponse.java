package com.expo.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TicketUpdateResponse(
        Long ticketProductId,
        BigDecimal price,
        int totalQuantity,
        int availableQuantity,
        Instant updatedAt) {}
