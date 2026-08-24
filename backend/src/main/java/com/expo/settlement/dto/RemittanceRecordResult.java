package com.expo.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 송금 결과 기록 응답.
 *
 * @param amountMatchesDue 이체 금액이 확정액과 같은가. <b>다르면 {@code false} 지만 거절하지 않는다</b> —
 *     이미 일어난 일을 기록하는 API 라 거절해 봐야 사실이 사라질 뿐이다. 대신 눈에 띄게 알린다
 */
@Schema(description = "송금 결과 기록 결과")
public record RemittanceRecordResult(
        @Schema(description = "정산 ID", example = "12") Long settlementId,
        @Schema(description = "기록된 송금 ID", example = "3") Long remittanceId,
        @Schema(description = "정산 상태. REMITTED 또는 REMITTANCE_PENDING") String settlementStatus,
        @Schema(description = "송금 상태", example = "REMITTED") String remittanceStatus,
        @Schema(description = "확정된 송금 예정액") BigDecimal remittanceDueAmount,
        @Schema(description = "실제 이체 금액") BigDecimal remittedAmount,
        @Schema(description = "두 금액이 일치하는가", example = "true") boolean amountMatchesDue,
        @Schema(description = "송금 완료 시각. 실패면 null") Instant remittedAt) {}
