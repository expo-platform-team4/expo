package com.expo.payment.dto;

import java.math.BigDecimal;

public record TicketPaymentResponse(
        Long paymentId, String clientKey, String orderId, String orderName, BigDecimal amount) {}
