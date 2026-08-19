package com.expo.ticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record TicketUpdateRequest(
        @NotNull @PositiveOrZero BigDecimal price,
        @NotNull @PositiveOrZero Integer totalQuantity) {}
