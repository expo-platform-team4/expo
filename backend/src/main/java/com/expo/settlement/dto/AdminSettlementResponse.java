package com.expo.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 관리자가 보는 정산 한 건 (D-API-013).
 *
 * <p>{@code v_admin_settlement_status} 의 출력이다. 클라이언트 응답과 달리 <b>업체명과 확정자</b>가
 * 있고, 대신 <b>금액 내역이 적다</b> — 목록에서 필요한 것은 "얼마를 언제까지 누구에게" 이지 구성
 * 내역이 아니다. 내역은 재계산 API 나 상세에서 본다.
 */
@Schema(description = "관리자 정산 현황")
public record AdminSettlementResponse(
        @Schema(description = "정산 ID", example = "12") Long settlementId,
        @Schema(description = "정산 상태", example = "CALCULATED") String status,
        @Schema(description = "박람회 ID", example = "7") Long expoId,
        @Schema(description = "박람회명") String expoTitle,
        @Schema(description = "행사 종료") Instant eventEndAt,
        @Schema(description = "주최사 ID (client_profiles.user_id)") Long hostClientId,
        @Schema(description = "주최사명") String companyName,
        @Schema(description = "정산 기한") Instant settlementDueAt,
        @Schema(description = "송금할 금액") BigDecimal remittanceDueAmount,
        @Schema(description = "조정 합계") BigDecimal adjustmentAmount,
        @Schema(description = "확정 시각") Instant confirmedAt,
        @Schema(description = "확정한 관리자 (users.id)") Long confirmedBy,
        @Schema(description = "실제 송금액") BigDecimal remittedAmount,
        @Schema(description = "송금 시각") Instant remittedAt,
        @Schema(description = "송금 상태") String remittanceStatus) {}
