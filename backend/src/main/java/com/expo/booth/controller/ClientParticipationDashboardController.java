package com.expo.booth.controller;

import com.expo.booth.dto.ClientDashboardBoothResponse;
import com.expo.booth.service.ClientParticipationDashboardService;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 클라이언트 마이페이지 - 참여기업 신청 목록 (E-API-008).
 *
 * <p>URL은 {@code /api/client/me/**} 이지만 데이터 소유 도메인(booth)에 맞춰 이 패키지에 둔다.
 */
@Tag(name = "Client Dashboard - Participation", description = "클라이언트 마이페이지 - 참여기업 신청 현황")
@RestController
@RequestMapping("/api/client/me")
public class ClientParticipationDashboardController {

    private final ClientParticipationDashboardService clientParticipationDashboardService;

    public ClientParticipationDashboardController(
            ClientParticipationDashboardService clientParticipationDashboardService) {
        this.clientParticipationDashboardService = clientParticipationDashboardService;
    }

    @Operation(
            summary = "참여기업 신청 목록 조회",
            description = "로그인한 클라이언트 본인이 참여기업으로 신청한 공고·결제·부스 목록을 조회합니다.")
    @GetMapping("/participations")
    public ResponseEntity<ApiResponse<List<ClientDashboardBoothResponse>>> getMyParticipations(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientParticipationDashboardService.getMyParticipations(
                                principal.getMemberId())));
    }
}
