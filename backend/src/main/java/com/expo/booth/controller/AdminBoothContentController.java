package com.expo.booth.controller;

import com.expo.booth.dto.BoothContentReasonRequest;
import com.expo.booth.dto.BoothContentResponse;
import com.expo.booth.dto.BoothManagementHistoryResponse;
import com.expo.booth.dto.RequestBoothContentCorrectionRequest;
import com.expo.booth.service.AdminBoothContentService;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자용 부스 콘텐츠 조회와 운영 확인·보완 요청·숨김 처리. */
@Tag(name = "Admin Booth Content", description = "관리자 부스 콘텐츠 조회·운영 처리")
@RestController
@RequestMapping("/api/admin/booth-contents")
public class AdminBoothContentController {

    private final AdminBoothContentService adminBoothContentService;

    public AdminBoothContentController(AdminBoothContentService adminBoothContentService) {
        this.adminBoothContentService = adminBoothContentService;
    }

    @Operation(summary = "부스 콘텐츠 목록 조회 (페이지 단위)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BoothContentResponse>>> list(
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(adminBoothContentService.list(pageable)));
    }

    @Operation(summary = "부스 콘텐츠 상세 조회")
    @GetMapping("/{contentId}")
    public ResponseEntity<ApiResponse<BoothContentResponse>> get(@PathVariable Long contentId) {
        return ResponseEntity.ok(ApiResponse.ok(adminBoothContentService.get(contentId)));
    }

    @Operation(summary = "부스 콘텐츠 운영 확인")
    @PostMapping("/{contentId}/check")
    public ResponseEntity<ApiResponse<BoothContentResponse>> check(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long contentId) {
        return ResponseEntity.ok(
                ApiResponse.ok(adminBoothContentService.check(contentId, principal.getMemberId())));
    }

    @Operation(summary = "부스 콘텐츠 보완 요청")
    @PostMapping("/{contentId}/request-correction")
    public ResponseEntity<ApiResponse<BoothContentResponse>> requestCorrection(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long contentId,
            @Valid @RequestBody RequestBoothContentCorrectionRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        adminBoothContentService.requestCorrection(
                                contentId, principal.getMemberId(), request.message())));
    }

    @Operation(summary = "부스 콘텐츠 직권 숨김")
    @PostMapping("/{contentId}/hide")
    public ResponseEntity<ApiResponse<BoothContentResponse>> hide(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long contentId,
            @RequestBody(required = false) BoothContentReasonRequest request) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(
                ApiResponse.ok(
                        adminBoothContentService.hide(contentId, principal.getMemberId(), reason)));
    }

    @Operation(summary = "부스 콘텐츠 숨김 해제")
    @PostMapping("/{contentId}/restore")
    public ResponseEntity<ApiResponse<BoothContentResponse>> restore(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long contentId,
            @RequestBody(required = false) BoothContentReasonRequest request) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(
                ApiResponse.ok(
                        adminBoothContentService.restore(
                                contentId, principal.getMemberId(), reason)));
    }

    @Operation(summary = "부스 콘텐츠 운영 변경 이력 조회")
    @GetMapping("/{contentId}/histories")
    public ResponseEntity<ApiResponse<List<BoothManagementHistoryResponse>>> listHistory(
            @PathVariable Long contentId) {
        return ResponseEntity.ok(ApiResponse.ok(adminBoothContentService.listHistory(contentId)));
    }
}
