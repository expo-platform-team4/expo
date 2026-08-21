package com.expo.refund.controller;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.refund.dto.TicketRefundRequest;
import com.expo.refund.dto.TicketRefundResponse;
import com.expo.refund.service.TicketRefundRequestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 회원 티켓 주문 환불 요청 API. */
@RestController
@RequiredArgsConstructor
@Tag(name = "티켓 환불 API")
@RequestMapping("/api/members/me/orders")
public class TicketRefundController {

    private final TicketRefundRequestService ticketRefundRequestService;

    @PostMapping("/{orderId}/refunds")
    public ResponseEntity<ApiResponse<TicketRefundResponse>> requestMemberRefund(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long orderId,
            @Valid @RequestBody TicketRefundRequest request) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        TicketRefundResponse response =
                ticketRefundRequestService.requestMemberRefund(orderId, request.reason(), principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
