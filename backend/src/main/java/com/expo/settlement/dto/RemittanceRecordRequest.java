package com.expo.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 외부 송금 결과 기록 요청 (D-API-016).
 *
 * <p>시스템이 송금하지 않는다. 담당자가 은행에서 이체하고 <b>그 결과를 적는 것</b>이라, 금액도
 * 상태도 요청이 알려 준다.
 *
 * @param status {@code REMITTED} / {@code FAILED} / {@code PENDING} / {@code PROCESSING} /
 *     {@code CANCELED}
 * @param remittedAmount 실제 이체한 금액. 확정액과 <b>다를 수 있다</b> — 수수료를 떼였거나 분할
 *     이체한 경우다. 다르면 응답이 알려 준다
 * @param referenceNumber 은행 거래번호. 증빙이라 성공 기록에는 넣는 것을 권한다
 */
@Schema(description = "송금 결과 기록")
public record RemittanceRecordRequest(
        @Schema(description = "송금 상태", example = "REMITTED") @NotNull(message = "송금 상태는 필수입니다.")
                String status,
        @Schema(description = "실제 이체 금액", example = "20000")
                @PositiveOrZero(message = "송금액은 0 이상이어야 합니다.")
                BigDecimal remittedAmount,
        @Schema(description = "은행 거래번호", example = "TRX-20260819-0001") @Size(max = 100)
                String referenceNumber,
        @Schema(description = "메모") String memo) {}
