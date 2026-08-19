package com.expo.settlement.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.settlement.dto.AdminSettlementResponse;
import com.expo.settlement.dto.RemittanceRecordRequest;
import com.expo.settlement.dto.RemittanceRecordResult;
import com.expo.settlement.dto.SettlementCalculationResult;
import com.expo.settlement.dto.SettlementConfirmResult;
import com.expo.settlement.dto.SettlementPage;
import com.expo.settlement.service.SettlementCalculationService;
import com.expo.settlement.service.SettlementConfirmService;
import com.expo.settlement.service.SettlementQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 정산 운영 (D-API-014). 인가는 {@code SecurityConfig} 가 경로로 건다. */
@Tag(name = "Admin Settlement", description = "관리자 정산 재계산·확정·송금")
@RestController
@RequestMapping("/api/admin/settlements")
public class AdminSettlementController {

    private final SettlementCalculationService settlementCalculationService;
    private final SettlementQueryService settlementQueryService;
    private final SettlementConfirmService settlementConfirmService;

    public AdminSettlementController(
            SettlementCalculationService settlementCalculationService,
            SettlementQueryService settlementQueryService,
            SettlementConfirmService settlementConfirmService) {
        this.settlementCalculationService = settlementCalculationService;
        this.settlementQueryService = settlementQueryService;
        this.settlementConfirmService = settlementConfirmService;
    }

    @Operation(
            summary = "정산 대상 목록·검색",
            description = "전체 정산을 정산 기한이 임박한 순서로 반환합니다. " + "상태·주최사·박람회·기한으로 필터링할 수 있습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<SettlementPage<AdminSettlementResponse>>> searchSettlements(
            @Parameter(
                            description =
                                    "WAITING / CALCULATED / UNDER_REVIEW / CONFIRMED / "
                                            + "REMITTANCE_PENDING / REMITTED / ON_HOLD")
                    @RequestParam(required = false)
                    String status,
            @Parameter(description = "주최사 ID") @RequestParam(required = false) Long hostClientId,
            @Parameter(description = "박람회 ID") @RequestParam(required = false) Long expoId,
            @Parameter(description = "이 시각 이전 기한만")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant dueBefore,
            @Parameter(description = "0부터") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "최대 100") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        settlementQueryService.searchSettlements(
                                status, hostClientId, expoId, dueBefore, page, size)));
    }

    @Operation(
            summary = "서버 기준 정산액 재계산",
            description =
                    "결제·환불·부스 매출·확정 조정을 다시 집계해 정산 금액을 덮어씁니다. "
                            + "여러 번 호출해도 누적되지 않습니다. "
                            + "확정(CONFIRMED) 이후에는 재계산할 수 없습니다 — 금액을 고쳐야 하면 조정으로 남깁니다. "
                            + "예매 수수료는 구매자가 추가로 낸 플랫폼 몫이라 송금액에 포함되지 않습니다.")
    @PostMapping("/{settlementId}/calculate")
    public ResponseEntity<ApiResponse<SettlementCalculationResult>> calculate(
            @Parameter(description = "정산 ID", required = true) @PathVariable Long settlementId) {
        return ResponseEntity.ok(
                ApiResponse.ok(settlementCalculationService.calculate(settlementId)));
    }

    @Operation(
            summary = "정산 검토·확정",
            description =
                    "정산 금액을 확정합니다. 계산이 끝난 정산(CALCULATED·UNDER_REVIEW)만 확정할 수 있습니다 — "
                            + "WAITING 은 금액이 0이라 확정하면 0원으로 굳습니다. "
                            + "확정 후에는 재계산할 수 없고 되돌리는 API도 없습니다. "
                            + "금액을 고쳐야 하면 조정으로 남깁니다.")
    @PostMapping("/{settlementId}/confirm")
    public ResponseEntity<ApiResponse<SettlementConfirmResult>> confirm(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "정산 ID", required = true) @PathVariable Long settlementId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        settlementConfirmService.confirm(settlementId, principal.getMemberId())));
    }

    @Operation(
            summary = "외부 송금 결과 기록",
            description =
                    "담당자가 은행에서 이체한 결과를 기록합니다. 시스템이 송금하지 않습니다. "
                            + "확정된 정산만 기록할 수 있습니다. "
                            + "이체 금액이 확정액과 달라도 거절하지 않고 amountMatchesDue 로 알려 줍니다 — "
                            + "이미 일어난 일이라 거절해도 사실이 사라지지 않습니다.")
    @PostMapping("/{settlementId}/transfers")
    public ResponseEntity<ApiResponse<RemittanceRecordResult>> recordRemittance(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "정산 ID", required = true) @PathVariable Long settlementId,
            @Valid @RequestBody RemittanceRecordRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        settlementConfirmService.recordRemittance(
                                settlementId, request, principal.getMemberId())));
    }
}
