package com.expo.ticket.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.ticket.dto.GuestTicketOrderReponse;
import com.expo.ticket.dto.GuestTicketOrderRequest;
import com.expo.ticket.dto.GuestTicketSearchRequest;
import com.expo.ticket.dto.GuestTicketSearchResponse;
import com.expo.ticket.dto.MemberTicketOrderRequest;
import com.expo.ticket.dto.TicketOrderResponse;
import com.expo.ticket.service.GuestTicketOrderSearchService;
import com.expo.ticket.service.TicketOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "TickeOrder", description = "티켓 주문 생성 API")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class TicketOrderController {

    private final TicketOrderService ticketOrderService;
    private final GuestTicketOrderSearchService guestTicketOrderSearchService;

    @PostMapping("/member")
    public ResponseEntity<ApiResponse<TicketOrderResponse>> memberTicketOrder(
            @AuthenticationPrincipal AuthPrincipal authPrincipal,
            @Valid @RequestBody MemberTicketOrderRequest request) {
        TicketOrderResponse response =
                ticketOrderService.memberCreateOrder(authPrincipal.getMemberId(), request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/guest")
    public ResponseEntity<ApiResponse<GuestTicketOrderReponse>> guestTicketOrder(
            @Valid @RequestBody GuestTicketOrderRequest request) {
        GuestTicketOrderReponse response = ticketOrderService.guestCreateOrder(request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/search/guest")
    public ResponseEntity<ApiResponse<GuestTicketSearchResponse>> guestTicketSearch(
            @Valid @RequestBody GuestTicketSearchRequest request) {
        GuestTicketSearchResponse response =
                guestTicketOrderSearchService.guestTicketSearch(request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
