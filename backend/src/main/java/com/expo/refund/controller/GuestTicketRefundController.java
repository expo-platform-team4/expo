package com.expo.refund.controller;

import com.expo.common.response.ApiResponse;
import com.expo.refund.dto.GuestTicketRefundEligibilityRequest;
import com.expo.refund.dto.GuestTicketRefundRequest;
import com.expo.refund.dto.TicketRefundEligibilityResponse;
import com.expo.refund.dto.TicketRefundResponse;
import com.expo.refund.service.GuestTicketRefundEligibilityService;
import com.expo.refund.service.GuestTicketRefundRequestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 비회원 티켓 주문 환불 요청 API. */
@RestController
@RequiredArgsConstructor
@Tag(name = "티켓 환불 API")
@RequestMapping("/api/orders/guest")
public class GuestTicketRefundController {

    private final GuestTicketRefundRequestService guestTicketRefundRequestService;
    private final GuestTicketRefundEligibilityService guestTicketRefundEligibilityService;

    @PostMapping("/refund-eligibility")
    public ResponseEntity<ApiResponse<TicketRefundEligibilityResponse>> checkRefundEligibility(
            @Valid @RequestBody GuestTicketRefundEligibilityRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(guestTicketRefundEligibilityService.check(request)));
    }

    @PostMapping("/refunds")
    public ResponseEntity<ApiResponse<TicketRefundResponse>> requestGuestRefund(
            @Valid @RequestBody GuestTicketRefundRequest request) {
        TicketRefundResponse response = guestTicketRefundRequestService.request(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
