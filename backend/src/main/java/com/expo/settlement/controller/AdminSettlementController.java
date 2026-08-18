package com.expo.settlement.controller;

import com.expo.common.response.ApiResponse;
import com.expo.settlement.dto.SettlementCalculationResult;
import com.expo.settlement.service.SettlementCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 정산 운영 (D-API-014). 인가는 {@code SecurityConfig} 가 경로로 건다. */
@Tag(name = "Admin Settlement", description = "관리자 정산 재계산·확정·송금")
@RestController
@RequestMapping("/api/admin/settlements")
public class AdminSettlementController {

    private final SettlementCalculationService settlementCalculationService;

    public AdminSettlementController(SettlementCalculationService settlementCalculationService) {
        this.settlementCalculationService = settlementCalculationService;
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
}
