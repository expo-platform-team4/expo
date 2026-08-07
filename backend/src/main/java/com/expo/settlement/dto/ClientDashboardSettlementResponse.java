package com.expo.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/** 박람회별 최종 정산 리포트 ({@code v_client_dashboard_settlements} 뷰 기반). */
@Schema(description = "클라이언트 마이페이지 - 정산 현황")
public record ClientDashboardSettlementResponse(
        @Schema(description = "주최 클라이언트 ID") Long hostClientId,
        @Schema(description = "정산 ID") Long settlementId,
        @Schema(description = "박람회 ID") Long expoId,
        @Schema(description = "박람회명") String expoTitle,
        @Schema(description = "행사 종료 시각") Instant eventEndAt,
        @Schema(description = "정산 예정 시각") Instant settlementDueAt,
        @Schema(description = "정산 상태") String status,
        @Schema(description = "티켓 판매 총액") BigDecimal grossTicketSalesAmount,
        @Schema(description = "티켓 환불액") BigDecimal ticketRefundAmount,
        @Schema(description = "티켓 판매 순액") BigDecimal netTicketSalesAmount,
        @Schema(description = "예매 수수료 총액") BigDecimal bookingFeeGrossAmount,
        @Schema(description = "예매 수수료 환불액") BigDecimal bookingFeeRefundAmount,
        @Schema(description = "예매 수수료 순액") BigDecimal bookingFeeNetAmount,
        @Schema(description = "부스 판매 총액") BigDecimal grossBoothSalesAmount,
        @Schema(description = "조정 금액") BigDecimal adjustmentAmount,
        @Schema(description = "송금 예정 금액") BigDecimal remittanceDueAmount,
        @Schema(description = "송금 완료 금액") BigDecimal remittedAmount,
        @Schema(description = "송금 완료 시각") Instant remittedAt,
        @Schema(description = "송금 상태") String remittanceStatus,
        @Schema(description = "최신 정산 리포트 파일 ID") Long latestReportFileId,
        @Schema(description = "최신 정산 리포트 포맷") String latestReportFormat,
        @Schema(description = "최신 정산 리포트 버전") Integer latestReportVersion) {}
