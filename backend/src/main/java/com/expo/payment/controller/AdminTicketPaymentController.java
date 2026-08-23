package com.expo.payment.controller;

import com.expo.common.response.ApiResponse;
import com.expo.payment.dto.AdminTicketPaymentHistoryResponse;
import com.expo.payment.entity.TicketPaymentEventType;
import com.expo.payment.service.AdminTicketPaymentHistoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 티켓 결제 이력 API. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/ticket-payments")
public class AdminTicketPaymentController {
    private final AdminTicketPaymentHistoryService adminTicketPaymentHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminTicketPaymentHistoryResponse>>> search(
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) TicketPaymentEventType eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        adminTicketPaymentHistoryService.search(
                                orderNumber, eventType, page, size)));
    }
}
