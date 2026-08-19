package com.expo.expo.controller;

import com.expo.common.response.ApiResponse;
import com.expo.expo.dto.ExpoDto.OpeningRequestApprove;
import com.expo.expo.dto.ExpoDto.OpeningRequestReject;
import com.expo.expo.dto.ExpoDto.OpeningRequestResponse;
import com.expo.expo.entity.ExpoEnums.OpeningRequestStatus;
import com.expo.expo.service.ExpoService;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * 박람회 개최 신청 심사 API (관리자 전용) — API 명세 No.6~9.
 *
 * <p>경로 {@code /api/admin/expo-opening-requests}. SecurityConfig 의 {@code /api/admin/**} →
 * hasRole("ADMIN") 필터가 접근을 막으므로, 여기 도달한 요청은 관리자 권한이 검증된 상태다.
 */
@Tag(name = "Expo Opening Request - Admin", description = "박람회 개최 신청 심사 API (관리자)")
@RestController
@RequestMapping("/api/admin/expo-opening-requests")
public class ExpoOpeningRequestAdminController {

    private final ExpoService expoService;

    public ExpoOpeningRequestAdminController(ExpoService expoService) {
        this.expoService = expoService;
    }

    /** No.6 — 관리자 개최 신청 목록·검색 (EXPO-03, EXPO-19). */
    @Operation(summary = "개최 신청 목록·검색", description = "상태별 개최 신청을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OpeningRequestResponse>>> getList(
            @RequestParam(required = false) OpeningRequestStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok(expoService.getOpeningRequests(status, page, size)));
    }

    /** No.7 — 관리자 개최 신청 상세 조회 (EXPO-03, EXPO-19). */
    @Operation(summary = "개최 신청 상세", description = "개최 신청의 상세를 조회합니다.")
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<OpeningRequestResponse>> getDetail(
            @PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.ok(expoService.getOpeningRequestDetail(requestId)));
    }

    /** No.8 — 개최 신청 승인·장소예약·박람회 자동 공개 (EXPO-03, EXPO-09, EXPO-19). */
    @Operation(summary = "개최 신청 승인", description = "승인 시 공개 박람회가 생성되어 목록에 노출됩니다.")
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ApiResponse<Long>> approve(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long requestId,
            @Valid @RequestBody OpeningRequestApprove request) {
        Long expoId =
                expoService.approveOpeningRequest(principal.getMemberId(), requestId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(expoId));
    }

    /** No.9 — 개최 신청 단순 반려 및 사유 저장 (EXPO-03~04). */
    @Operation(summary = "개최 신청 반려", description = "신청을 반려하고 사유를 기록합니다.")
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long requestId,
            @Valid @RequestBody OpeningRequestReject request) {
        expoService.rejectOpeningRequest(principal.getMemberId(), requestId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
