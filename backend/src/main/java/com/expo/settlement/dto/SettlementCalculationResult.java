package com.expo.settlement.dto;

import com.expo.settlement.entity.SettlementAmounts;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * 재계산 결과. 관리자가 <b>숫자가 어떻게 나왔는지</b> 바로 볼 수 있게 구성 요소를 전부 준다.
 *
 * <p>송금액만 주면 "왜 이 금액이지" 를 확인하려고 DB 를 열어야 한다. 정산은 금액이 틀리면 돈이
 * 잘못 나가는 기능이라, 계산 근거가 응답에 있어야 한다.
 */
@Schema(description = "정산 재계산 결과")
public record SettlementCalculationResult(
        @Schema(description = "정산 ID", example = "12") Long settlementId,
        @Schema(description = "재계산 뒤 상태", example = "CALCULATED") String status,
        @Schema(description = "티켓 판매원금") BigDecimal grossTicketSalesAmount,
        @Schema(description = "티켓 환불원금") BigDecimal ticketRefundAmount,
        @Schema(description = "티켓 순매출. 판매원금 − 환불원금") BigDecimal netTicketSalesAmount,
        @Schema(description = "예매 수수료. 구매자가 추가로 낸 플랫폼 몫") BigDecimal bookingFeeGrossAmount,
        @Schema(description = "예매 수수료 환불") BigDecimal bookingFeeRefundAmount,
        @Schema(description = "예매 수수료 순액. 송금액에 포함되지 않는다") BigDecimal bookingFeeNetAmount,
        @Schema(description = "부스 매출. 플랫폼 수수료 0원이라 전액") BigDecimal grossBoothSalesAmount,
        @Schema(description = "PG 수수료 참고값. 현재 항상 0") BigDecimal pgFeeReferenceAmount,
        @Schema(description = "확정된 조정의 합") BigDecimal adjustmentAmount,
        @Schema(description = "송금할 금액. 티켓 순매출 + 부스 매출 + 조정") BigDecimal remittanceDueAmount) {

    public static SettlementCalculationResult of(
            Long settlementId, String status, SettlementAmounts amounts) {
        return new SettlementCalculationResult(
                settlementId,
                status,
                amounts.grossTicketSalesAmount(),
                amounts.ticketRefundAmount(),
                amounts.netTicketSalesAmount(),
                amounts.bookingFeeGrossAmount(),
                amounts.bookingFeeRefundAmount(),
                amounts.bookingFeeNetAmount(),
                amounts.grossBoothSalesAmount(),
                amounts.pgFeeReferenceAmount(),
                amounts.adjustmentAmount(),
                amounts.remittanceDueAmount());
    }
}
