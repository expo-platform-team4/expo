package com.expo.member.controller;

import com.expo.jwt.AuthPrincipal;
import com.expo.member.dto.ClientDashboardProfileResponse;
import com.expo.member.dto.MemberApiResponse;
import com.expo.member.service.ClientDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 클라이언트 마이페이지 (E-API-001). */
@Tag(name = "Client Dashboard", description = "클라이언트 마이페이지 API")
@RestController
@RequestMapping("/api/client/me")
public class ClientDashboardController {

    private final ClientDashboardService clientDashboardService;

    public ClientDashboardController(ClientDashboardService clientDashboardService) {
        this.clientDashboardService = clientDashboardService;
    }

    @Operation(
            summary = "클라이언트 마이페이지 요약 조회",
            description = "로그인한 클라이언트 본인의 마이페이지 상단 요약(프로필)을 조회합니다.")
    @GetMapping("/dashboard")
    public ResponseEntity<MemberApiResponse<ClientDashboardProfileResponse>> getDashboardSummary(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(
                MemberApiResponse.ok(
                        clientDashboardService.getDashboardSummary(principal.getMemberId())));
    }
}
