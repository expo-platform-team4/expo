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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 주최자용 모집 결과 조회·확인. */
@Tag(name = "Client Recruitment Result", description = "주최자 모집 결과 조회·확인")
@RestController
@RequestMapping("/api/client/recruitment-results")
public class ClientRecruitmentResultController {

    private final ClientRecruitmentResultService clientRecruitmentResultService;

    public ClientRecruitmentResultController(
            ClientRecruitmentResultService clientRecruitmentResultService) {
        this.clientRecruitmentResultService = clientRecruitmentResultService;
    }

    @Operation(summary = "내 모집 결과 상세 조회")
    @GetMapping("/{resultId}")
    public ResponseEntity<ApiResponse<RecruitmentResultResponse>> getMine(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long resultId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientRecruitmentResultService.getMine(resultId, principal.getMemberId())));
    }

    @Operation(summary = "모집 결과 확인")
    @PostMapping("/{resultId}/confirm")
    public ResponseEntity<ApiResponse<RecruitmentResultResponse>> confirm(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long resultId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientRecruitmentResultService.confirm(resultId, principal.getMemberId())));
    }
}
