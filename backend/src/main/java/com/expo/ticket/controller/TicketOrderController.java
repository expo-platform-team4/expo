package com.expo.ticket.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.ticket.dto.MemberTicketOrderCreateRequest;
import com.expo.ticket.dto.TicketOrderResponse;
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

    @PostMapping("/member")
    public ResponseEntity<ApiResponse<TicketOrderResponse>> memberTicketOrder(
            @AuthenticationPrincipal AuthPrincipal authPrincipal,
            @Valid @RequestBody MemberTicketOrderCreateRequest request) {
        TicketOrderResponse response =
                ticketOrderService.memberCreateOrder(authPrincipal.getMemberId(), request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
