package com.expo.recruitment.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.recruitment.dto.RecruitmentNoticeRequestResponse;
import com.expo.recruitment.service.RecruitmentNoticeRequestService;
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
 * 주최 클라이언트의 모집공고 생성 요청 조회. 작성은 관리자가 승인된 박람회를 골라 대신 한다({@code
 * AdminRecruitmentNoticeRequestController}) — 클라이언트는 결과만 조회한다.
 */
@Tag(name = "Client Recruitment Notice Request", description = "주최 클라이언트 모집공고 생성 요청 조회")
@RestController
@RequestMapping("/api/client/recruitment-notice-requests")
public class ClientRecruitmentNoticeRequestController {

    private final RecruitmentNoticeRequestService recruitmentNoticeRequestService;

    public ClientRecruitmentNoticeRequestController(
            RecruitmentNoticeRequestService recruitmentNoticeRequestService) {
        this.recruitmentNoticeRequestService = recruitmentNoticeRequestService;
    }

    @Operation(summary = "내 모집공고 생성 요청 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecruitmentNoticeRequestResponse>>> getMine(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(recruitmentNoticeRequestService.listMine(principal.getMemberId())));
    }

    @Operation(summary = "내 모집공고 생성 요청 상세 조회")
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<RecruitmentNoticeRequestResponse>> getMine(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        recruitmentNoticeRequestService.getMine(
                                requestId, principal.getMemberId())));
    }
}
