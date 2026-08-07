package com.expo.expo.controller;

import com.expo.expo.dto.ClientDashboardExpoResponse;
import com.expo.expo.service.ClientExpoDashboardService;
import com.expo.jwt.AuthPrincipal;
import com.expo.member.dto.MemberApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 클라이언트 마이페이지 - 등록 박람회 목록 (E-API-002).
 *
 * <p>URL은 {@code /api/client/me/**} 이지만 데이터 소유 도메인(expo)에 맞춰 이 패키지에 둔다.
 * {@code SecurityConfig}의 {@code /api/client/**} (CLIENT, ADMIN) 규칙을 그대로 탄다.
 */
@Tag(name = "Client Dashboard - Expo", description = "클라이언트 마이페이지 - 등록 박람회")
@RestController
@RequestMapping("/api/client/me")
public class ClientExpoDashboardController {

    private final ClientExpoDashboardService clientExpoDashboardService;

    public ClientExpoDashboardController(ClientExpoDashboardService clientExpoDashboardService) {
        this.clientExpoDashboardService = clientExpoDashboardService;
    }

    @Operation(summary = "내 등록 박람회 목록 조회", description = "로그인한 클라이언트 본인이 등록한 박람회 목록을 조회합니다.")
    @GetMapping("/expos")
    public ResponseEntity<MemberApiResponse<List<ClientDashboardExpoResponse>>> getMyExpos(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(
                MemberApiResponse.ok(
                        clientExpoDashboardService.getMyExpos(principal.getMemberId())));
    }
}
