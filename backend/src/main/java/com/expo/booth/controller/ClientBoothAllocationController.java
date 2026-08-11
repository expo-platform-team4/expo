package com.expo.booth.controller;

import com.expo.booth.dto.BoothAllocationResponse;
import com.expo.booth.service.BoothAllocationService;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 참여 기업의 부스 확정 배정 조회. */
@Tag(name = "Client Booth Allocation", description = "참여 기업 부스 확정 배정 조회")
@RestController
@RequestMapping("/api/client/booth-allocations")
public class ClientBoothAllocationController {

    private final BoothAllocationService boothAllocationService;

    public ClientBoothAllocationController(BoothAllocationService boothAllocationService) {
        this.boothAllocationService = boothAllocationService;
    }

    @Operation(summary = "내 부스 확정 배정 상세 조회")
    @GetMapping("/{allocationId}")
    public ResponseEntity<ApiResponse<BoothAllocationResponse>> getMine(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long allocationId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        boothAllocationService.getMine(allocationId, principal.getMemberId())));
    }
}
