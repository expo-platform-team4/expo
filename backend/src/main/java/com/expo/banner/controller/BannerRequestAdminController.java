package com.expo.banner.controller;

import com.expo.banner.dto.ActiveBannerResponse;
import com.expo.banner.dto.BannerApplicationAdminResponse;
import com.expo.banner.dto.BannerApplicationRejectRequest;
import com.expo.banner.dto.BannerApplicationResponse;
import com.expo.banner.entity.BannerApplication.ReviewStatus;
import com.expo.banner.service.BannerApplicationService;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 광고 배너 신청 심사 API (관리자 전용) — B-API-021~023.
 *
 * <p>경로 {@code /api/admin/banner-requests}. SecurityConfig 의 {@code /api/admin/**} →
 * hasRole("ADMIN") 필터가 접근을 막으므로, 여기 도달한 요청은 관리자 권한이 검증된 상태다
 * (ExpoOpeningRequestAdminController 와 동일 컨벤션).
 */
@Tag(name = "Banner Request - Admin", description = "광고 배너 신청 심사 API (관리자)")
@RestController
@RequestMapping("/api/admin/banner-requests")
public class BannerRequestAdminController {

    private final BannerApplicationService bannerApplicationService;

    public BannerRequestAdminController(BannerApplicationService bannerApplicationService) {
        this.bannerApplicationService = bannerApplicationService;
    }

    /** B-API-021 — 배너 신청 목록·기간 충돌 조회. 심사 대기 건은 항목마다 충돌 여부를 함께 내려준다. */
    @Operation(summary = "배너 신청 목록·기간 충돌 조회", description = "상태별 배너 신청과 노출 기간 충돌 여부를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BannerApplicationAdminResponse>>> getList(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        bannerApplicationService.getApplicationsForAdmin(status, page, size)));
    }

    /** B-API-021A — 배너 신청 상세 조회. */
    @Operation(summary = "배너 신청 상세", description = "배너 신청의 상세를 조회합니다.")
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<BannerApplicationResponse>> getDetail(
            @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.ok(bannerApplicationService.getApplicationDetail(requestId)));
    }

    /**
     * 목록의 {@code hasPeriodConflict} 드릴다운 — 실제로 겹치는 배너 목록을 반환한다.
     * (WBS 번호는 없지만 B-API-021 "기간 충돌 조회" 요구를 상세화하는 보조 엔드포인트.)
     */
    @Operation(summary = "배너 신청 기간 충돌 상세", description = "이 신청과 노출 기간이 겹치는 배너 목록을 조회합니다.")
    @GetMapping("/{requestId}/conflicts")
    public ResponseEntity<ApiResponse<List<ActiveBannerResponse>>> getConflicts(
            @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.ok(bannerApplicationService.getConflictingBanners(requestId)));
    }

    /** B-API-022 — 배너 승인 및 즉시/예약 자동 활성화. */
    @Operation(summary = "배너 신청 승인", description = "승인 시 실제 노출 배너가 생성되어 즉시/예약으로 활성화됩니다.")
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ApiResponse<Long>> approve(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long requestId) {
        Long bannerId =
                bannerApplicationService.approveApplication(principal.getMemberId(), requestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(bannerId));
    }

    /** B-API-023 — 배너 신청 반려. */
    @Operation(summary = "배너 신청 반려", description = "신청을 반려하고 사유를 기록합니다.")
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long requestId,
            @Valid @RequestBody BannerApplicationRejectRequest request) {
        bannerApplicationService.rejectApplication(principal.getMemberId(), requestId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
