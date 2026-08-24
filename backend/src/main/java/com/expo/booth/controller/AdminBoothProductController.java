package com.expo.booth.controller;

import com.expo.booth.dto.BoothProductResponse;
import com.expo.booth.dto.BulkCreateBoothProductRequest;
import com.expo.booth.dto.CreateBoothProductRequest;
import com.expo.booth.dto.UpdateBoothProductSalesStatusRequest;
import com.expo.booth.service.BoothProductService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자용 부스 상품 등록·조회·판매 상태 관리. */
@Tag(name = "Admin Booth Product", description = "관리자 부스 상품 등록·조회·판매 상태 관리")
@RestController
@RequestMapping("/api/admin/booth-products")
public class AdminBoothProductController {

    private final BoothProductService boothProductService;

    public AdminBoothProductController(BoothProductService boothProductService) {
        this.boothProductService = boothProductService;
    }

    @Operation(summary = "부스 상품 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<BoothProductResponse>> create(
            @Valid @RequestBody CreateBoothProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(boothProductService.create(request)));
    }

    @Operation(summary = "부스 상품 일괄 등록")
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<BoothProductResponse>>> createBulk(
            @Valid @RequestBody BulkCreateBoothProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(boothProductService.createBulk(request.boothProducts())));
    }

    @Operation(summary = "공고별 부스 상품 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BoothProductResponse>>> list(
            @RequestParam Long recruitmentNoticeId) {
        return ResponseEntity.ok(
                ApiResponse.ok(boothProductService.listForAdmin(recruitmentNoticeId)));
    }

    @Operation(summary = "부스 상품 상세 조회")
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<BoothProductResponse>> get(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(boothProductService.get(productId)));
    }

    @Operation(summary = "부스 상품 판매 상태 변경")
    @PatchMapping("/{productId}/sales-status")
    public ResponseEntity<ApiResponse<BoothProductResponse>> updateSalesStatus(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateBoothProductSalesStatusRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        boothProductService.updateSalesStatus(productId, request.salesStatus())));
    }
}
