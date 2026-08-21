package com.expo.payment.controller;

import com.expo.common.response.ApiResponse;
import com.expo.payment.dto.TicketPaymentRequest;
import com.expo.payment.dto.TicketPaymentResponse;
import com.expo.payment.service.TicketPaymentInitiationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "티켓 결제 API")
@RequestMapping("/api/payments/")
public class TicketPaymentController {

    private final TicketPaymentInitiationService ticketPaymentInitiationService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<TicketPaymentResponse>> initiatePayment(
            @Valid @RequestBody TicketPaymentRequest request) {
        TicketPaymentResponse response =
                ticketPaymentInitiationService.initiate(request.orderNumber());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
