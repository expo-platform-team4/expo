package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 토스 결제창에서 돌아온 뒤 결제 승인을 요청할 때 보내는 값. */
@Schema(description = "부스 상품 결제 승인 요청")
public record ConfirmBoothPaymentRequest(
        @Schema(description = "토스 결제 키") @NotBlank(message = "결제 키는 필수입니다.") String paymentKey,
        @Schema(description = "토스에 전달했던 주문 ID") @NotBlank(message = "주문 ID는 필수입니다.") String orderId,
        @Schema(description = "결제 금액") @NotNull(message = "결제 금액은 필수입니다.") BigDecimal amount) {}
