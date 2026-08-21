package com.expo.settlement.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.settlement.dto.ClientSettlementDetail;
import com.expo.settlement.dto.ClientSettlementResponse;
import com.expo.settlement.dto.SettlementPage;
import com.expo.settlement.service.SettlementQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주최사 정산 조회 (D-API-011 · 012).
 *
 * <p>인가는 {@code SecurityConfig} 가 경로로 건다 — {@code /api/client/**} 는 {@code CLIENT} 또는
 * {@code ADMIN} 이다. 다만 <b>역할만으로는 부족하다.</b> 다른 주최사도 {@code CLIENT} 이므로,
 * 실제 소유 판정은 서비스가 {@code hostClientId} 를 조건에 넣어 한다.
 */
@Tag(name = "Client Settlement", description = "주최사 정산 조회")
@RestController
@RequestMapping("/api/client/settlements")
public class ClientSettlementController {

    private final SettlementQueryService settlementQueryService;

    public ClientSettlementController(SettlementQueryService settlementQueryService) {
        this.settlementQueryService = settlementQueryService;
    }

    @Operation(
            summary = "내 정산 목록 조회",
            description =
                    "로그인한 주최사의 정산을 정산 기한이 임박한 순서로 반환합니다. "
                            + "예매 수수료는 구매자가 추가로 낸 플랫폼 몫이라 받을 금액에 포함되지 않습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<SettlementPage<ClientSettlementResponse>>> findMySettlements(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "0부터") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "최대 100") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        settlementQueryService.findMySettlements(
                                principal.getMemberId(), page, size)));
    }

    @Operation(
            summary = "정산 상세·티켓/부스 구분 리포트 조회",
            description =
                    "정산 요약과 금액 구성 항목을 함께 반환합니다. 항목은 티켓 판매·환불, 예매 수수료, "
                            + "부스 매출, 조정으로 갈리며 includedInRemittance 가 정산금 포함 여부를 알려 줍니다. "
                            + "내 정산이 아니면 404입니다.")
    @GetMapping("/{settlementId}")
    public ResponseEntity<ApiResponse<ClientSettlementDetail>> findMySettlement(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "정산 ID", required = true) @PathVariable Long settlementId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        settlementQueryService.findMySettlement(
                                settlementId, principal.getMemberId())));
    }
}
