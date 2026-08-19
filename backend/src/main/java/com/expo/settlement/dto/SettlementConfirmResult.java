package com.expo.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 정산 확정 결과 (D-API-015).
 *
 * <p>확정된 금액을 함께 돌려준다. 확정은 <b>되돌릴 수 없는 조작</b>이라, 무엇을 확정했는지가 응답에
 * 남아야 관리자가 곧바로 대사할 수 있다.
 */
@Schema(description = "정산 확정 결과")
public record SettlementConfirmResult(
        @Schema(description = "정산 ID", example = "12") Long settlementId,
        @Schema(description = "확정 뒤 상태", example = "CONFIRMED") String status,
        @Schema(description = "확정된 송금액") BigDecimal remittanceDueAmount,
        @Schema(description = "확정 시각") Instant confirmedAt,
        @Schema(description = "확정한 관리자 (users.id)") Long confirmedBy) {}
