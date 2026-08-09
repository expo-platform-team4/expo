package com.expo.recruitment.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.recruitment.dto.CreateRecruitmentNoticeRequest;
import com.expo.recruitment.dto.RecruitmentNoticeResponse;
import com.expo.recruitment.service.RecruitmentNoticeService;
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

/** 관리자용 기업 모집 공고 작성·조회. */
@Tag(name = "Admin Recruitment Notice", description = "관리자 기업 모집 공고 작성·조회")
@RestController
@RequestMapping("/api/admin/recruitment-notices")
public class AdminRecruitmentNoticeController {

    private final RecruitmentNoticeService recruitmentNoticeService;

    public AdminRecruitmentNoticeController(RecruitmentNoticeService recruitmentNoticeService) {
        this.recruitmentNoticeService = recruitmentNoticeService;
    }

    @Operation(summary = "기업 모집 공고 초안 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<RecruitmentNoticeResponse>> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateRecruitmentNoticeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                recruitmentNoticeService.create(principal.getMemberId(), request)));
    }

    @Operation(summary = "기업 모집 공고 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecruitmentNoticeResponse>>> getNotices() {
        return ResponseEntity.ok(ApiResponse.ok(recruitmentNoticeService.list()));
    }

    @Operation(summary = "기업 모집 공고 상세 조회")
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<RecruitmentNoticeResponse>> getNotice(
            @PathVariable Long noticeId) {
        return ResponseEntity.ok(ApiResponse.ok(recruitmentNoticeService.get(noticeId)));
    }
}
