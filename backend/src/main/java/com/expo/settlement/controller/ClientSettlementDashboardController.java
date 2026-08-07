package com.expo.settlement.controller;

import com.expo.jwt.AuthPrincipal;
import com.expo.member.dto.MemberApiResponse;
import com.expo.settlement.dto.ClientDashboardDailySalesResponse;
import com.expo.settlement.service.ClientSettlementDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 클라이언트 마이페이지 - 박람회별 판매 현황 (E-API-003).
 *
 * <p>URL은 {@code /api/client/me/**} 이지만 데이터 소유 도메인(settlement)에 맞춰 이 패키지에 둔다.
 */
@Tag(name = "Client Dashboard - Settlement", description = "클라이언트 마이페이지 - 판매 현황")
@RestController
@RequestMapping("/api/client/me")
public class ClientSettlementDashboardController {

    private final ClientSettlementDashboardService clientSettlementDashboardService;

    public ClientSettlementDashboardController(
            ClientSettlementDashboardService clientSettlementDashboardService) {
        this.clientSettlementDashboardService = clientSettlementDashboardService;
    }

    @Operation(summary = "박람회별 판매 현황 조회", description = "로그인한 클라이언트 본인 소유 박람회의 일자별 판매 현황을 조회합니다.")
    @GetMapping("/expos/{expoId}/sales-summary")
    public ResponseEntity<MemberApiResponse<List<ClientDashboardDailySalesResponse>>>
            getMyDailySales(
                    @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long expoId) {
        return ResponseEntity.ok(
                MemberApiResponse.ok(
                        clientSettlementDashboardService.getMyDailySales(
                                principal.getMemberId(), expoId)));
    }
}
