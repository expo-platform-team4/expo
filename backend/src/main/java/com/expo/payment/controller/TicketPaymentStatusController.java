package com.expo.payment.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.payment.dto.TicketPaymentStatusResponse;
import com.expo.payment.service.TicketPaymentStatusService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 결제 상태 재조회 API. */
@RestController
@RequiredArgsConstructor
@Tag(name = "티켓 결제 API")
@RequestMapping("/api/orders")
public class TicketPaymentStatusController {

    private final TicketPaymentStatusService ticketPaymentStatusService;

    @GetMapping("/{orderNumber}/payment-status")
    public ResponseEntity<ApiResponse<TicketPaymentStatusResponse>> getPaymentStatus(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable String orderNumber) {
        TicketPaymentStatusResponse response =
                ticketPaymentStatusService.getStatus(orderNumber, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
