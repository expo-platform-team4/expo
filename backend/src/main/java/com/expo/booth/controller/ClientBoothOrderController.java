package com.expo.booth.controller;

import com.expo.booth.dto.BoothOrderResponse;
import com.expo.booth.dto.CreateBoothOrderRequest;
import com.expo.booth.service.BoothOrderService;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 참여 기업의 부스 상품 주문 생성·조회·취소. */
@Tag(name = "Client Booth Order", description = "참여 기업 부스 상품 주문 생성·조회·취소")
@RestController
@RequestMapping("/api/client/booth-orders")
public class ClientBoothOrderController {

    private final BoothOrderService boothOrderService;

    public ClientBoothOrderController(BoothOrderService boothOrderService) {
        this.boothOrderService = boothOrderService;
    }

    @Operation(summary = "부스 상품 주문 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<BoothOrderResponse>> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateBoothOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                boothOrderService.create(
                                        request.applicationId(), principal.getMemberId())));
    }

    @Operation(summary = "내 부스 상품 주문 상세 조회")
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<BoothOrderResponse>> getMine(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long orderId) {
        return ResponseEntity.ok(
                ApiResponse.ok(boothOrderService.getMine(orderId, principal.getMemberId())));
    }

    @Operation(summary = "결제 전 부스 상품 주문 취소")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<BoothOrderResponse>> cancel(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long orderId) {
        return ResponseEntity.ok(
                ApiResponse.ok(boothOrderService.cancel(orderId, principal.getMemberId())));
    }
}
