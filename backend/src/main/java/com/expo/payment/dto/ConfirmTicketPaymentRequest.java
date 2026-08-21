package com.expo.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 토스 결제창에서 돌아온 뒤 티켓 결제 승인을 요청할 때 보내는 값. */
public record ConfirmTicketPaymentRequest(
        @NotBlank(message = "결제 키는 필수입니다.") String paymentKey,
        @NotBlank(message = "주문 ID는 필수입니다.") String orderId,
        @NotNull(message = "결제 금액은 필수입니다.") BigDecimal amount) {}
