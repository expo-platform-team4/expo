package com.expo.ticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GuestTicketOrderRequest(
        @NotEmpty @Size(max = 4) @Valid List<TicketOrderItemRequest> items,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 20) String phoneNumber,
        @Positive int age,
        @NotBlank String password) {}
