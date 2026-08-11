package com.expo.participation.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.participation.dto.AdminParticipationApplicationResponse;
import com.expo.participation.dto.ApplicationOperationHistoryResponse;
import com.expo.participation.dto.OperationMessageRequest;
import com.expo.participation.dto.RequestCorrectionRequest;
import com.expo.participation.dto.UpdateMemoRequest;
import com.expo.participation.service.AdminParticipationApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자용 참여 신청서 조회와 운영 확인·보완 요청 처리. */
@Tag(name = "Admin Participation Application", description = "관리자 참여 신청서 조회·운영 확인")
@RestController
@RequestMapping("/api/admin/participation-applications")
public class AdminParticipationApplicationController {

    private final AdminParticipationApplicationService adminParticipationApplicationService;

    public AdminParticipationApplicationController(
            AdminParticipationApplicationService adminParticipationApplicationService) {
        this.adminParticipationApplicationService = adminParticipationApplicationService;
    }

    @Operation(summary = "참여 신청서 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminParticipationApplicationResponse>>> list(
            @RequestParam(required = false) Long recruitmentNoticeId) {
        return ResponseEntity.ok(
                ApiResponse.ok(adminParticipationApplicationService.list(recruitmentNoticeId)));
    }

    @Operation(summary = "참여 신청서 상세 조회")
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<AdminParticipationApplicationResponse>> get(
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(
                ApiResponse.ok(adminParticipationApplicationService.get(applicationId)));
    }

    @Operation(summary = "운영 확인 처리")
    @PostMapping("/{applicationId}/check")
    public ResponseEntity<ApiResponse<AdminParticipationApplicationResponse>> check(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long applicationId,
            @RequestBody(required = false) OperationMessageRequest request) {
        String message = request != null ? request.message() : null;
        return ResponseEntity.ok(
                ApiResponse.ok(
                        adminParticipationApplicationService.check(
                                applicationId, principal.getMemberId(), message)));
    }

    @Operation(summary = "보완 요청")
    @PostMapping("/{applicationId}/correction-request")
    public ResponseEntity<ApiResponse<AdminParticipationApplicationResponse>> requestCorrection(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long applicationId,
            @Valid @RequestBody RequestCorrectionRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        adminParticipationApplicationService.requestCorrection(
                                applicationId, principal.getMemberId(), request.message())));
    }

    @Operation(summary = "보완 완료 처리")
    @PostMapping("/{applicationId}/correction-complete")
    public ResponseEntity<ApiResponse<AdminParticipationApplicationResponse>> completeCorrection(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long applicationId,
            @RequestBody(required = false) OperationMessageRequest request) {
        String message = request != null ? request.message() : null;
        return ResponseEntity.ok(
                ApiResponse.ok(
                        adminParticipationApplicationService.completeCorrection(
                                applicationId, principal.getMemberId(), message)));
    }

    @Operation(summary = "관리자 메모 갱신")
    @PatchMapping("/{applicationId}/memo")
    public ResponseEntity<ApiResponse<AdminParticipationApplicationResponse>> updateMemo(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateMemoRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        adminParticipationApplicationService.updateMemo(
                                applicationId, principal.getMemberId(), request.memo())));
    }

    @Operation(summary = "운영 확인·보완 요청 이력 조회")
    @GetMapping("/{applicationId}/histories")
    public ResponseEntity<ApiResponse<List<ApplicationOperationHistoryResponse>>> listHistory(
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(
                ApiResponse.ok(adminParticipationApplicationService.listHistory(applicationId)));
    }
}
