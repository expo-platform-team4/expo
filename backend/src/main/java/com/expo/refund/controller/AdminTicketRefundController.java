package com.expo.refund.controller;

import com.expo.common.response.ApiResponse;
import com.expo.refund.service.TicketRefundRetryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 티켓 환불 재처리 API. */
@RestController
@RequiredArgsConstructor
@Tag(name = "관리자 티켓 환불 API")
@RequestMapping("/api/admin/ticket-refunds")
public class AdminTicketRefundController {

    private final TicketRefundRetryService ticketRefundRetryService;

    @PostMapping("/{refundId}/retry")
    public ResponseEntity<ApiResponse<Void>> retry(@PathVariable Long refundId) {
        ticketRefundRetryService.retry(refundId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
