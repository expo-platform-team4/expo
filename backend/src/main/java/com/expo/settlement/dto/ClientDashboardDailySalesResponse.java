package com.expo.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** 박람회별 일자별 티켓 판매·환불 현황 ({@code v_client_dashboard_daily_sales} 뷰 기반). */
@Schema(description = "클라이언트 마이페이지 - 일자별 판매 현황")
public record ClientDashboardDailySalesResponse(
        @Schema(description = "클라이언트 사용자 ID") Long clientUserId,
        @Schema(description = "박람회 ID") Long expoId,
        @Schema(description = "박람회명") String expoTitle,
        @Schema(description = "집계 일자") LocalDate salesDate,
        @Schema(description = "결제 완료 주문 수") int paidOrderCount,
        @Schema(description = "취소 주문 수") int canceledOrderCount,
        @Schema(description = "판매 티켓 수량") int soldTicketQuantity,
        @Schema(description = "환불 티켓 수량") int refundTicketQuantity,
        @Schema(description = "티켓 판매 금액") BigDecimal ticketSalesAmount,
        @Schema(description = "예매 수수료 금액") BigDecimal bookingFeeAmount,
        @Schema(description = "환불 티켓 금액") BigDecimal refundTicketAmount,
        @Schema(description = "환불 예매 수수료 금액") BigDecimal refundBookingFeeAmount,
        @Schema(description = "구매자 결제 금액") BigDecimal buyerPaymentAmount,
        @Schema(description = "클라이언트 정산 기준 금액") BigDecimal clientSettlementBaseAmount,
        @Schema(description = "집계 시각") Instant calculatedAt) {}
