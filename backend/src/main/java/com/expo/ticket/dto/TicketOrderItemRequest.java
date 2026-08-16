package com.expo.ticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TicketOrderItemRequest(
        @NotNull Long ticketProductId, @Positive @Max(4) int quantity) {}
