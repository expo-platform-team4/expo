package com.expo.expo.controller;

import com.expo.common.response.ApiResponse;
import com.expo.expo.dto.ExpoOpeningRequestResponse;
import com.expo.expo.dto.RejectExpoOpeningRequestRequest;
import com.expo.expo.entity.ExpoOpeningRequestStatus;
import com.expo.expo.service.ExpoOpeningRequestService;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자의 박람회 개최 승인 관리 (이슈 #116). */
@Tag(name = "Admin Expo Opening Request", description = "관리자 박람회 개최 승인 관리")
@RestController
@RequestMapping("/api/admin/expo-opening-requests")
@RequiredArgsConstructor
public class AdminExpoOpeningRequestController {

    private final ExpoOpeningRequestService expoOpeningRequestService;

    @Operation(summary = "개최 신청 목록", description = "status 를 주면 그 상태만, 없으면 전체.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpoOpeningRequestResponse>>> list(
            @RequestParam(required = false) ExpoOpeningRequestStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(expoOpeningRequestService.listForAdmin(status)));
    }

    @Operation(summary = "개최 신청 상세")
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<ExpoOpeningRequestResponse>> get(
            @PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.ok(expoOpeningRequestService.getForAdmin(requestId)));
    }

    @Operation(summary = "개최 승인", description = "신청을 승인하고 같은 트랜잭션에서 expos 행과 심사 이력을 만든다.")
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ApiResponse<ExpoOpeningRequestResponse>> approve(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        expoOpeningRequestService.approve(requestId, principal.getMemberId())));
    }

    @Operation(summary = "개최 반려", description = "사유는 주최사에게 그대로 보인다.")
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<ExpoOpeningRequestResponse>> reject(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long requestId,
            @Valid @RequestBody RejectExpoOpeningRequestRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        expoOpeningRequestService.reject(
                                requestId, principal.getMemberId(), request.reason())));
    }
}
