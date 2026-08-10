package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** 부스 상품 결제 시작 응답. 프런트가 이 값으로 토스페이먼츠 결제창을 띄운다. */
@Schema(description = "부스 상품 결제 시작 응답")
public record InitiateBoothPaymentResponse(
        @Schema(description = "결제 ID") Long boothPaymentId,
        @Schema(description = "토스 클라이언트 키") String clientKey,
        @Schema(description = "토스에 전달할 주문 ID") String pgOrderId,
        @Schema(description = "주문명") String orderName,
        @Schema(description = "결제 금액") BigDecimal amount) {}
