package com.expo.booth.controller;

import com.expo.booth.dto.BoothAllocationResponse;
import com.expo.booth.dto.CancelBoothAllocationRequest;
import com.expo.booth.dto.ReassignBoothAllocationRequest;
import com.expo.booth.service.BoothAllocationService;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/** 관리자용 부스 확정 배정 조회·취소·재배정. */
@Tag(name = "Admin Booth Allocation", description = "관리자 부스 확정 배정 조회·취소·재배정")
@RestController
@RequestMapping("/api/admin/booth-allocations")
public class AdminBoothAllocationController {

    private final BoothAllocationService boothAllocationService;

    public AdminBoothAllocationController(BoothAllocationService boothAllocationService) {
        this.boothAllocationService = boothAllocationService;
    }

    @Operation(summary = "부스 확정 배정 목록 조회 (페이지 단위)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BoothAllocationResponse>>> list(
            @PageableDefault(sort = "allocatedAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(boothAllocationService.listForAdmin(pageable)));
    }

    @Operation(summary = "부스 확정 배정 상세 조회")
    @GetMapping("/{allocationId}")
    public ResponseEntity<ApiResponse<BoothAllocationResponse>> get(
            @PathVariable Long allocationId) {
        return ResponseEntity.ok(ApiResponse.ok(boothAllocationService.getForAdmin(allocationId)));
    }

    @Operation(summary = "부스 확정 배정 취소 (이중 배정 등 운영상 정정 전용)")
    @PostMapping("/{allocationId}/cancel")
    public ResponseEntity<ApiResponse<BoothAllocationResponse>> cancel(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long allocationId,
            @Valid @RequestBody CancelBoothAllocationRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        boothAllocationService.cancel(
                                allocationId, principal.getMemberId(), request.reason())));
    }

    @Operation(summary = "부스 확정 배정 재배정 (이중 배정 등 운영상 정정, 다른 부스 상품으로 이동)")
    @PostMapping("/{allocationId}/reassign")
    public ResponseEntity<ApiResponse<BoothAllocationResponse>> reassign(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long allocationId,
            @Valid @RequestBody ReassignBoothAllocationRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        boothAllocationService.reassign(
                                allocationId,
                                request.boothProductId(),
                                principal.getMemberId(),
                                request.reason())));
    }
}
