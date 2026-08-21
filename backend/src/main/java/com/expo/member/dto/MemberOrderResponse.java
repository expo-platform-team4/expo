package com.expo.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 일반 회원 마이페이지 주문 목록·상세 요약 (A-API-019, A-API-020, {@code v_member_mypage_orders} 뷰 기반).
 *
 * <p>{@code paymentStatus}·{@code refundStatus}는 해당 주문의 가장 최근 결제·환불 이력만 담는다(뷰에서
 * 이미 최신 1건으로 좁혀 온다). 결제·환불 이력이 아예 없으면 {@code null}이다.
 */
@Schema(description = "내 주문 요약")
public record MemberOrderResponse(
        @Schema(description = "주문 ID") Long orderId,
        @Schema(description = "주문번호") String orderNumber,
        @Schema(description = "박람회 ID") Long expoId,
        @Schema(description = "박람회명") String expoTitle,
        @Schema(description = "주문 상태") String orderStatus,
        @Schema(description = "결제 상태 (최신 1건, 없으면 null)") String paymentStatus,
        @Schema(description = "환불 상태 (최신 1건, 없으면 null)") String refundStatus,
        @Schema(description = "총 수량") int totalQuantity,
        @Schema(description = "티켓 소계 금액") BigDecimal ticketSubtotalAmount,
        @Schema(description = "예매 수수료율") BigDecimal bookingFeeRate,
        @Schema(description = "예매 수수료 금액") BigDecimal bookingFeeAmount,
        @Schema(description = "총 결제 금액") BigDecimal totalAmount,
        @Schema(description = "환불 가능 여부 (행사 3일 전 & 미사용)") boolean refundable,
        @Schema(description = "주문 생성 시각") Instant createdAt) {}
