package com.expo.booth.controller;

import com.expo.booth.dto.BoothPaymentResponse;
import com.expo.booth.dto.ConfirmBoothPaymentRequest;
import com.expo.booth.dto.InitiateBoothPaymentResponse;
import com.expo.booth.service.BoothPaymentService;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 참여 기업의 부스 상품 주문 결제(토스페이먼츠) 시작·승인·조회. */
@Tag(name = "Client Booth Payment", description = "참여 기업 부스 상품 결제 시작·승인·조회")
@RestController
@RequestMapping("/api/client")
public class ClientBoothPaymentController {

    private final BoothPaymentService boothPaymentService;

    public ClientBoothPaymentController(BoothPaymentService boothPaymentService) {
        this.boothPaymentService = boothPaymentService;
    }

    @Operation(summary = "부스 상품 결제 시작")
    @PostMapping("/booth-orders/{orderId}/payments")
    public ResponseEntity<ApiResponse<InitiateBoothPaymentResponse>> initiate(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long orderId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                boothPaymentService.initiate(orderId, principal.getMemberId())));
    }

    @Operation(summary = "부스 상품 결제 승인")
    @PostMapping("/booth-payments/confirm")
    public ResponseEntity<ApiResponse<BoothPaymentResponse>> confirm(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ConfirmBoothPaymentRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        boothPaymentService.confirm(
                                request.orderId(),
                                request.paymentKey(),
                                request.amount(),
                                principal.getMemberId())));
    }

    @Operation(summary = "주문별 결제 시도 목록 조회")
    @GetMapping("/booth-orders/{orderId}/payments")
    public ResponseEntity<ApiResponse<List<BoothPaymentResponse>>> listMine(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long orderId) {
        return ResponseEntity.ok(
                ApiResponse.ok(boothPaymentService.listMine(orderId, principal.getMemberId())));
    }
}
