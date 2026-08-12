package com.expo.booth.controller;

import com.expo.booth.dto.BoothProductResponse;
import com.expo.booth.service.BoothProductService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 공개 부스 상품 조회. 참여를 검토하는 기업이 구매 가능한 상품만 본다. */
@Tag(name = "Public Booth Product", description = "공개 부스 상품 조회")
@RestController
@RequestMapping("/api/public/booth-products")
public class PublicBoothProductController {

    private final BoothProductService boothProductService;

    public PublicBoothProductController(BoothProductService boothProductService) {
        this.boothProductService = boothProductService;
    }

    @Operation(summary = "공고별 구매 가능한 부스 상품 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BoothProductResponse>>> listAvailable(
            @RequestParam Long recruitmentNoticeId) {
        return ResponseEntity.ok(
                ApiResponse.ok(boothProductService.listAvailable(recruitmentNoticeId)));
    }
}
