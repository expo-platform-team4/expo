package com.expo.expo.controller;

import com.expo.common.response.ApiResponse;
import com.expo.expo.dto.CreateExpoOpeningRequestRequest;
import com.expo.expo.dto.ExpoOpeningRequestResponse;
import com.expo.expo.dto.UpdateExpoOpeningRequestRequest;
import com.expo.expo.service.ExpoOpeningRequestService;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 주최사의 박람회 개최 신청 (이슈 #116). */
@Tag(name = "Client Expo Opening Request", description = "주최사 박람회 개최 신청")
@RestController
@RequestMapping("/api/client/expo-opening-requests")
@RequiredArgsConstructor
public class ClientExpoOpeningRequestController {

    private final ExpoOpeningRequestService expoOpeningRequestService;

    @Operation(
            summary = "박람회 개최 신청 작성",
            description = "submitNow=false 면 임시저장(DRAFT), true 면 바로 심사 요청(SUBMITTED).")
    @PostMapping
    public ResponseEntity<ApiResponse<ExpoOpeningRequestResponse>> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateExpoOpeningRequestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                expoOpeningRequestService.create(
                                        principal.getMemberId(), request)));
    }

    @Operation(summary = "내 개최 신청 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpoOpeningRequestResponse>>> listMine(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(expoOpeningRequestService.listMine(principal.getMemberId())));
    }

    @Operation(summary = "내 개최 신청 상세")
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<ExpoOpeningRequestResponse>> getMine(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        expoOpeningRequestService.getMine(requestId, principal.getMemberId())));
    }

    @Operation(summary = "개최 신청 수정", description = "임시저장 상태에서만 가능하다.")
    @PatchMapping("/{requestId}")
    public ResponseEntity<ApiResponse<ExpoOpeningRequestResponse>> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long requestId,
            @Valid @RequestBody UpdateExpoOpeningRequestRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        expoOpeningRequestService.update(
                                requestId, principal.getMemberId(), request)));
    }

    @Operation(summary = "심사 요청", description = "임시저장 → 심사 요청.")
    @PostMapping("/{requestId}/submit")
    public ResponseEntity<ApiResponse<ExpoOpeningRequestResponse>> submit(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        expoOpeningRequestService.submit(requestId, principal.getMemberId())));
    }

    @Operation(summary = "개최 신청 철회", description = "심사 결과가 나오기 전에만 가능하다.")
    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<ApiResponse<ExpoOpeningRequestResponse>> cancel(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        expoOpeningRequestService.cancel(requestId, principal.getMemberId())));
    }
}
