package com.expo.recruitment.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.recruitment.dto.CreateRecruitmentNoticeRequestRequest;
import com.expo.recruitment.dto.RecruitmentNoticeRequestResponse;
import com.expo.recruitment.service.RecruitmentNoticeRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 주최 클라이언트의 모집공고 생성 요청 작성·조회. */
@Tag(name = "Client Recruitment Notice Request", description = "주최 클라이언트 모집공고 생성 요청")
@RestController
@RequestMapping("/api/client/recruitment-notice-requests")
public class ClientRecruitmentNoticeRequestController {

    private final RecruitmentNoticeRequestService recruitmentNoticeRequestService;

    public ClientRecruitmentNoticeRequestController(
            RecruitmentNoticeRequestService recruitmentNoticeRequestService) {
        this.recruitmentNoticeRequestService = recruitmentNoticeRequestService;
    }

    @Operation(summary = "모집공고 생성 요청 작성")
    @PostMapping
    public ResponseEntity<ApiResponse<RecruitmentNoticeRequestResponse>> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateRecruitmentNoticeRequestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                recruitmentNoticeRequestService.create(
                                        principal.getMemberId(), request)));
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
