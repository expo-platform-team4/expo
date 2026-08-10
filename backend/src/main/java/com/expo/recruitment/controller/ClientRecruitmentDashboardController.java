package com.expo.recruitment.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.recruitment.dto.ClientDashboardRecruitmentResponse;
import com.expo.recruitment.service.ClientRecruitmentDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 클라이언트 마이페이지 - 모집공고 결과 목록 (E-API-005).
 *
 * <p>URL은 {@code /api/client/me/**} 이지만 데이터 소유 도메인(recruitment)에 맞춰 이 패키지에 둔다.
 */
@Tag(name = "Client Dashboard - Recruitment", description = "클라이언트 마이페이지 - 모집공고 결과")
@RestController
@RequestMapping("/api/client/me")
public class ClientRecruitmentDashboardController {

    private final ClientRecruitmentDashboardService clientRecruitmentDashboardService;

    public ClientRecruitmentDashboardController(
            ClientRecruitmentDashboardService clientRecruitmentDashboardService) {
        this.clientRecruitmentDashboardService = clientRecruitmentDashboardService;
    }

    @Operation(
            summary = "모집 완료 결과 목록 조회",
            description = "로그인한 클라이언트 본인이 등록한 모집공고와 신청·확정 배정 건수를 조회합니다.")
    @GetMapping("/recruitment-results")
    public ResponseEntity<ApiResponse<List<ClientDashboardRecruitmentResponse>>>
            getMyRecruitmentNotices(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientRecruitmentDashboardService.getMyRecruitmentNotices(
                                principal.getMemberId())));
    }
}
