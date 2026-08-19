package com.expo.recruitment.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.recruitment.dto.RecruitmentResultResponse;
import com.expo.recruitment.service.ClientRecruitmentResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 주최자용 모집 결과 조회. 전달된 결과를 조회하면 그 즉시 확인 처리된다. */
@Tag(name = "Client Recruitment Result", description = "주최자 모집 결과 조회 (조회 시 자동 확인)")
@RestController
@RequestMapping("/api/client/recruitment-results")
public class ClientRecruitmentResultController {

    private final ClientRecruitmentResultService clientRecruitmentResultService;

    public ClientRecruitmentResultController(
            ClientRecruitmentResultService clientRecruitmentResultService) {
        this.clientRecruitmentResultService = clientRecruitmentResultService;
    }

    @Operation(summary = "내 모집 결과 상세 조회 (전달 상태였다면 조회와 동시에 확인 처리)")
    @GetMapping("/{resultId}")
    public ResponseEntity<ApiResponse<RecruitmentResultResponse>> getMine(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long resultId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientRecruitmentResultService.getMine(resultId, principal.getMemberId())));
    }
}
