package com.expo.ticket.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.ticket.dto.PurchasableTicketProductResponse;
import com.expo.ticket.dto.TicketProductCreateRequest;
import com.expo.ticket.dto.TicketProductCreateResponse;
import com.expo.ticket.dto.TicketProductSearchResponse;
import com.expo.ticket.dto.TicketProductStatusUpdateRequest;
import com.expo.ticket.dto.TicketUpdateRequest;
import com.expo.ticket.dto.TicketUpdateResponse;
import com.expo.ticket.service.PurchasableTicketProductService;
import com.expo.ticket.service.TicketProductCreateService;
import com.expo.ticket.service.TicketProductStatusService;
import com.expo.ticket.service.TicketProductUpdateService;
import com.expo.ticket.service.TicketSearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// import org.springframework.web.bind.annotation.*;

@Tag(name = "Ticket", description = "티켓 API")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class TicketController {

    private final TicketProductCreateService ticketProductCreateService;
    private final TicketSearchService ticketSearchService;
    private final TicketProductUpdateService ticketProductUpdateService;
    private final TicketProductStatusService ticketProductStatusService;
    private final PurchasableTicketProductService purchasableTicketProductService;

    @PostMapping("/client/expos/{expoId}/ticket-products")
    public ResponseEntity<ApiResponse<TicketProductCreateResponse>> createTicket(
            @AuthenticationPrincipal AuthPrincipal authPrincipal,
            @PathVariable Long expoId,
            @Valid @RequestBody TicketProductCreateRequest request) {
        TicketProductCreateResponse response =
                ticketProductCreateService.ticketCreate(
                        expoId, authPrincipal.getMemberId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/client/expos/{expoId}/ticket-search")
    public ResponseEntity<ApiResponse<List<TicketProductSearchResponse>>> searchTickets(
            @AuthenticationPrincipal AuthPrincipal authPrincipal, @PathVariable Long expoId) {
        List<TicketProductSearchResponse> response =
                ticketSearchService.ticketProductSearch(authPrincipal.getMemberId(), expoId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/client/expos/{expoId}/ticket-products/{ticketProductId}")
    public ResponseEntity<ApiResponse<TicketUpdateResponse>> updateTicket(
            @AuthenticationPrincipal AuthPrincipal authPrincipal,
            @PathVariable Long ticketProductId,
            @PathVariable Long expoId,
            @Valid @RequestBody TicketUpdateRequest request) {
        TicketUpdateResponse response =
                ticketProductUpdateService.ticketUpdate(
                        authPrincipal.getMemberId(), expoId, ticketProductId, request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/client/expos/{expoId}/ticket-products/{ticketProductId}/status")
    public ResponseEntity<ApiResponse<TicketProductSearchResponse>> updateStatus(
            @AuthenticationPrincipal AuthPrincipal authPrincipal,
            @PathVariable Long expoId,
            @PathVariable Long ticketProductId,
            @Valid @RequestBody TicketProductStatusUpdateRequest request) {
        TicketProductSearchResponse response =
                ticketProductStatusService.changeStatus(
                        authPrincipal.getMemberId(), expoId, ticketProductId, request.status());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/expos/{expoId}/ticket-products/purchasable")
    public ResponseEntity<ApiResponse<List<PurchasableTicketProductResponse>>>
            searchPurchasableTickets(@PathVariable Long expoId) {
        List<PurchasableTicketProductResponse> responses =
                purchasableTicketProductService.purchasableTicket(expoId);

        return ResponseEntity.ok(ApiResponse.ok(responses));
    }
}
