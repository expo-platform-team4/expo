package com.expo.booth.dto;

import com.expo.booth.entity.BoothOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 부스 상품 주문 응답. */
@Schema(description = "부스 상품 주문")
public record BoothOrderResponse(
        @Schema(description = "주문 ID") Long id,
        @Schema(description = "참여 신청서 ID") Long applicationId,
        @Schema(description = "주문한 클라이언트 사용자 ID") Long clientUserId,
        @Schema(description = "부스 상품 ID") Long boothProductId,
        @Schema(description = "주문 번호") String orderNumber,
        @Schema(description = "단가") BigDecimal unitPrice,
        @Schema(description = "결제 총액") BigDecimal totalAmount,
        @Schema(description = "주문 상태") BoothOrderStatus status,
        @Schema(description = "주문 만료 일시") LocalDateTime expiresAt,
        @Schema(description = "결제 완료 일시") LocalDateTime paidAt,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt) {}
