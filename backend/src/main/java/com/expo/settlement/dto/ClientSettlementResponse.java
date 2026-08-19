package com.expo.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 주최사가 보는 정산 한 건 (D-API-011 · 012).
 *
 * <p>{@code v_client_dashboard_settlements} 의 출력을 그대로 옮긴 것이다. 관리자 응답과 달리
 * <b>업체명·확정자가 없다</b> — 자기 것만 보므로 누구 것인지 적을 이유가 없고, 누가 확정했는지는
 * 내부 운영 정보다.
 *
 * @param bookingFeeNetAmount 예매 수수료 순액. <b>정산금에 포함되지 않는다.</b> 구매자가 판매원금 위에
 *     추가로 낸 플랫폼 몫이라, 주최사에게는 "얼마가 수수료로 걷혔나" 를 보여 주는 참고값이다
 * @param latestReportFileId 최신 정산 리포트 파일. 파일 도메인이 아직 없어 항상 {@code null} 이다
 */
@Schema(description = "주최사 정산")
public record ClientSettlementResponse(
        @Schema(description = "정산 ID", example = "12") Long settlementId,
        @Schema(description = "박람회 ID", example = "7") Long expoId,
        @Schema(description = "박람회명") String expoTitle,
        @Schema(description = "행사 종료") Instant eventEndAt,
        @Schema(description = "정산 기한. 행사 종료 + 14일") Instant settlementDueAt,
        @Schema(
                        description =
                                "WAITING / CALCULATED / UNDER_REVIEW / CONFIRMED / "
                                        + "REMITTANCE_PENDING / REMITTED / ON_HOLD",
                        example = "CALCULATED")
                String status,
        @Schema(description = "티켓 판매원금") BigDecimal grossTicketSalesAmount,
        @Schema(description = "티켓 환불원금") BigDecimal ticketRefundAmount,
        @Schema(description = "티켓 순매출") BigDecimal netTicketSalesAmount,
        @Schema(description = "예매 수수료") BigDecimal bookingFeeGrossAmount,
        @Schema(description = "예매 수수료 환불") BigDecimal bookingFeeRefundAmount,
        @Schema(description = "예매 수수료 순액. 정산금에 포함되지 않는다") BigDecimal bookingFeeNetAmount,
        @Schema(description = "부스 매출") BigDecimal grossBoothSalesAmount,
        @Schema(description = "조정 합계") BigDecimal adjustmentAmount,
        @Schema(description = "받을 금액") BigDecimal remittanceDueAmount,
        @Schema(description = "실제 송금액. 아직이면 null") BigDecimal remittedAmount,
        @Schema(description = "송금 시각") Instant remittedAt,
        @Schema(description = "송금 상태") String remittanceStatus,
        @Schema(description = "최신 리포트 파일 ID") Long latestReportFileId,
        @Schema(description = "리포트 형식. PDF / EXCEL") String latestReportFormat,
        @Schema(description = "리포트 버전") Integer latestReportVersion) {}
