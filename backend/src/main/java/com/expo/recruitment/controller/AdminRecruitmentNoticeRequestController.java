package com.expo.recruitment.controller;

import com.expo.common.response.ApiResponse;
import com.expo.recruitment.dto.RecruitmentNoticeRequestResponse;
import com.expo.recruitment.service.RecruitmentNoticeRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자용 모집공고 생성 요청 조회. */
@Tag(name = "Admin Recruitment Notice Request", description = "관리자 모집공고 생성 요청 조회")
@RestController
@RequestMapping("/api/admin/recruitment-notice-requests")
public class AdminRecruitmentNoticeRequestController {

    private final RecruitmentNoticeRequestService recruitmentNoticeRequestService;

    public AdminRecruitmentNoticeRequestController(
            RecruitmentNoticeRequestService recruitmentNoticeRequestService) {
        this.recruitmentNoticeRequestService = recruitmentNoticeRequestService;
    }

    @Operation(summary = "모집공고 생성 요청 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecruitmentNoticeRequestResponse>>> getRequests() {
        return ResponseEntity.ok(ApiResponse.ok(recruitmentNoticeRequestService.listForAdmin()));
    }

    @Operation(summary = "모집공고 생성 요청 상세 조회")
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<RecruitmentNoticeRequestResponse>> getRequest(
            @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.ok(recruitmentNoticeRequestService.getForAdmin(requestId)));
    }
}
