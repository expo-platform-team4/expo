package com.expo.ticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GuestTicketOrderRequest(
        @NotEmpty @Size(max = 4) @Valid List<TicketOrderItemRequest> items,
        @NotBlank String name,
        @NotBlank String phoneNumber,
        @Positive int age,
        @NotBlank String password) {}
