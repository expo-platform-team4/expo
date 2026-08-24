package com.expo.booth.controller;

import com.expo.booth.dto.BoothResponse;
import com.expo.booth.dto.BulkCreateBoothRequest;
import com.expo.booth.dto.CreateBoothRequest;
import com.expo.booth.service.BoothService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 부스 공간 등록·조회. */
@Tag(name = "Admin Booth", description = "관리자 부스 공간 등록·조회")
@RestController
@RequestMapping("/api/admin/venue-zones/{zoneId}/booths")
public class BoothController {

    private final BoothService boothService;

    public BoothController(BoothService boothService) {
        this.boothService = boothService;
    }

    @Operation(summary = "구역 안에 부스 공간 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<BoothResponse>> createBooth(
            @PathVariable Long zoneId, @Valid @RequestBody CreateBoothRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(boothService.create(zoneId, request)));
    }

    @Operation(summary = "구역 안에 부스 공간 일괄 등록")
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<BoothResponse>>> createBoothsBulk(
            @PathVariable Long zoneId, @Valid @RequestBody BulkCreateBoothRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(boothService.createBulk(zoneId, request.booths())));
    }

    @Operation(summary = "구역 내 부스 공간 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BoothResponse>>> list(@PathVariable Long zoneId) {
        return ResponseEntity.ok(ApiResponse.ok(boothService.list(zoneId)));
    }

    @Operation(summary = "구역 내 부스 공간 상세 조회")
    @GetMapping("/{boothId}")
    public ResponseEntity<ApiResponse<BoothResponse>> get(
            @PathVariable Long zoneId, @PathVariable Long boothId) {
        return ResponseEntity.ok(ApiResponse.ok(boothService.get(zoneId, boothId)));
    }
}
