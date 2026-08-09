package com.expo.recruitment.controller;

import com.expo.common.response.ApiResponse;
import com.expo.recruitment.dto.RecruitmentNoticeResponse;
import com.expo.recruitment.service.RecruitmentNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 기업 모집 공고 열람. */
@Tag(name = "Recruitment Notice", description = "공개 기업 모집 공고 열람")
@RestController
@RequestMapping("/api/recruitment-notices")
public class PublicRecruitmentNoticeController {

    private final RecruitmentNoticeService recruitmentNoticeService;

    public PublicRecruitmentNoticeController(RecruitmentNoticeService recruitmentNoticeService) {
        this.recruitmentNoticeService = recruitmentNoticeService;
    }

    @Operation(summary = "게시 중인 기업 모집 공고 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecruitmentNoticeResponse>>> getNotices() {
        return ResponseEntity.ok(ApiResponse.ok(recruitmentNoticeService.listPublic()));
    }

    @Operation(summary = "기업 모집 공고 상세·참여조건 조회")
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<RecruitmentNoticeResponse>> getNotice(
            @PathVariable Long noticeId) {
        return ResponseEntity.ok(ApiResponse.ok(recruitmentNoticeService.getPublic(noticeId)));
    }
}
