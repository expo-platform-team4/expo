package com.expo.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestTicketSearchRequest(
        @NotBlank String orderNumber,
        @NotBlank @Size(max = 100) String password,
        @NotBlank @Size(max = 20) String phoneNumber) {}
