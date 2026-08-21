package com.expo.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record TicketPaymentRequest(@NotBlank String orderNumber) {}
