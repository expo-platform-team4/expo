package com.expo.booth.controller;

import com.expo.booth.dto.BoothContentResponse;
import com.expo.booth.service.ClientBoothContentService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 부스 콘텐츠 조회. 공개(PUBLISHED) 상태의 콘텐츠만 노출한다. */
@Tag(name = "Public Booth Content", description = "공개 부스 콘텐츠 조회")
@RestController
@RequestMapping("/api/public/booth-contents")
public class PublicBoothContentController {

    private final ClientBoothContentService clientBoothContentService;

    public PublicBoothContentController(ClientBoothContentService clientBoothContentService) {
        this.clientBoothContentService = clientBoothContentService;
    }

    @Operation(summary = "부스 확정 배정 ID로 공개 콘텐츠 조회")
    @GetMapping("/by-allocation/{boothAllocationId}")
    public ResponseEntity<ApiResponse<BoothContentResponse>> getPublished(
            @PathVariable Long boothAllocationId) {
        return ResponseEntity.ok(
                ApiResponse.ok(clientBoothContentService.getPublished(boothAllocationId)));
    }
}
