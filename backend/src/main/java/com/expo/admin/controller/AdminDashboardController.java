package com.expo.admin.controller;

import com.expo.admin.dto.AdminDashboardCountsResponse;
import com.expo.admin.dto.AdminPendingReviewResponse;
import com.expo.admin.service.AdminDashboardService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 대시보드 (E-API-010, E-API-011). */
@Tag(name = "Admin Dashboard", description = "관리자 대시보드")
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @Operation(
            summary = "관리자 대시보드 핵심 현황 조회",
            description = "박람회·배너 심사, 모집공고 생성 요청, 장소 충돌, 참여 신청 운영 확인, 정산 등 처리 대기 건수를 집계해 조회합니다.")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminDashboardCountsResponse>> getDashboardSummary() {
        return ResponseEntity.ok(ApiResponse.ok(adminDashboardService.getDashboardSummary()));
    }

    @Operation(summary = "관리자 대기 업무 조회", description = "박람회 개최·배너·모집공고 생성 요청 등 심사 대기 목록을 통합 조회합니다.")
    @GetMapping("/pending-tasks")
    public ResponseEntity<ApiResponse<List<AdminPendingReviewResponse>>> getPendingTasks() {
        return ResponseEntity.ok(ApiResponse.ok(adminDashboardService.getPendingTasks()));
    }
}
