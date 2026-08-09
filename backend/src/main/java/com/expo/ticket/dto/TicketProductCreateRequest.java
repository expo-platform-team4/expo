package com.expo.ticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record TicketProductCreateRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotNull Instant salesStartAt,
        @NotNull Instant salesEndAt,
        @PositiveOrZero @NotNull Integer totalQuantity,
        @Min(1) @Max(4) @NotNull Integer maxQuantityPerOrder) {}
