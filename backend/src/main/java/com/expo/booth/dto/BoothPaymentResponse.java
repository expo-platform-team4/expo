package com.expo.booth.dto;

import com.expo.booth.entity.BoothPaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/** 부스 상품 결제 응답. */
@Schema(description = "부스 상품 결제")
public record BoothPaymentResponse(
        @Schema(description = "결제 ID") Long id,
        @Schema(description = "주문 ID") Long boothOrderId,
        @Schema(description = "토스에 전달한 주문 ID") String pgOrderId,
        @Schema(description = "토스 결제 키") String paymentKey,
        @Schema(description = "결제 수단") String method,
        @Schema(description = "결제 상태") BoothPaymentStatus status,
        @Schema(description = "요청 금액") BigDecimal requestedAmount,
        @Schema(description = "승인 금액") BigDecimal approvedAmount,
        @Schema(description = "승인 일시") Instant approvedAt,
        @Schema(description = "마지막 실패 코드") String lastFailureCode,
        @Schema(description = "생성 일시") Instant createdAt,
        @Schema(description = "수정 일시") Instant updatedAt) {}
