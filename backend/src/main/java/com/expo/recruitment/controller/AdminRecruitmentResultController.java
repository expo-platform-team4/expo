package com.expo.recruitment.controller;

import com.expo.common.response.ApiResponse;
import com.expo.recruitment.dto.RecruitmentResultResponse;
import com.expo.recruitment.service.AdminRecruitmentResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자용 모집 결과 생성·조회·전달·직권 취소. */
@Tag(name = "Admin Recruitment Result", description = "관리자 모집 결과 생성·조회·전달·직권 취소")
@RestController
@RequestMapping("/api/admin/recruitment-results")
public class AdminRecruitmentResultController {

    private final AdminRecruitmentResultService adminRecruitmentResultService;

    public AdminRecruitmentResultController(
            AdminRecruitmentResultService adminRecruitmentResultService) {
        this.adminRecruitmentResultService = adminRecruitmentResultService;
    }

    @Operation(summary = "모집 결과 생성 (마감된 공고의 결제·배정 완료 기업 집계)")
    @PostMapping
    public ResponseEntity<ApiResponse<RecruitmentResultResponse>> generate(
            @RequestParam Long recruitmentNoticeId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(adminRecruitmentResultService.generate(recruitmentNoticeId)));
    }

    @Operation(summary = "모집 결과 목록 조회 (페이지 단위)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RecruitmentResultResponse>>> list(
            @PageableDefault(sort = "generatedAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(adminRecruitmentResultService.list(pageable)));
    }

    @Operation(summary = "모집 결과 상세 조회")
    @GetMapping("/{resultId}")
    public ResponseEntity<ApiResponse<RecruitmentResultResponse>> get(@PathVariable Long resultId) {
        return ResponseEntity.ok(ApiResponse.ok(adminRecruitmentResultService.get(resultId)));
    }

    @Operation(summary = "주최자에게 결과 전달")
    @PostMapping("/{resultId}/deliver")
    public ResponseEntity<ApiResponse<RecruitmentResultResponse>> deliver(
            @PathVariable Long resultId) {
        return ResponseEntity.ok(ApiResponse.ok(adminRecruitmentResultService.deliver(resultId)));
    }

    @Operation(summary = "모집 결과 직권 취소")
    @PostMapping("/{resultId}/cancel")
    public ResponseEntity<ApiResponse<RecruitmentResultResponse>> cancel(
            @PathVariable Long resultId) {
        return ResponseEntity.ok(ApiResponse.ok(adminRecruitmentResultService.cancel(resultId)));
    }
}
