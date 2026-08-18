package com.expo.booth.controller;

import com.expo.booth.dto.BoothResponse;
import com.expo.booth.service.BoothService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 부스 공간 조회. 구역 도면(배치도)을 그리는 데 필요한 부스 공간 정보를 노출한다. */
@Tag(name = "Public Booth", description = "공개 부스 공간 조회")
@RestController
@RequestMapping("/api/public/venue-zones/{zoneId}/booths")
public class PublicBoothController {

    private final BoothService boothService;

    public PublicBoothController(BoothService boothService) {
        this.boothService = boothService;
    }

    @Operation(summary = "구역 내 부스 공간 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BoothResponse>>> list(@PathVariable Long zoneId) {
        return ResponseEntity.ok(ApiResponse.ok(boothService.list(zoneId)));
    }
}
