package com.expo.ticket.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.ticket.dto.TicketProductCreateRequest;
import com.expo.ticket.dto.TicketProductCreateResponse;
import com.expo.ticket.dto.TicketProductSearchResponse;
import com.expo.ticket.service.TicketProductCreateService;
import com.expo.ticket.service.TicketSearchService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class TicketController {

    private final TicketProductCreateService ticketProductCreateService;
    private final TicketSearchService ticketSearchService;

    @PostMapping("/client/expos/{expoId}/ticket-products")
    public ResponseEntity<ApiResponse<TicketProductCreateResponse>> createTicket(
            @AuthenticationPrincipal AuthPrincipal authPrincipal,
            @PathVariable Long expoId,
            @Valid @RequestBody TicketProductCreateRequest request) {
        TicketProductCreateResponse response =
                ticketProductCreateService.ticketCreate(
                        expoId, authPrincipal.getMemberId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(response));
    }

    @GetMapping("/client/expos/{expoId}/ticket-search")
    public ResponseEntity<ApiResponse<List<TicketProductSearchResponse>>> searchTicket(
            @AuthenticationPrincipal AuthPrincipal authPrincipal, @PathVariable Long expoId) {
        List<TicketProductSearchResponse> response =
                ticketSearchService.ticketProductSearch(authPrincipal.getMemberId(), expoId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
