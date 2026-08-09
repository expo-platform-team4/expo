package com.expo.ticket.controller;


import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.ticket.dto.TicketProductCreateRequest;
import com.expo.ticket.dto.TicketProductCreateResponse;
import com.expo.ticket.service.TicketProductCreateService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class CreateTicketController {

    private final TicketProductCreateService ticketProductCreateService;

    @PostMapping("/client/expos/{expoId}/ticket-products")
    public ResponseEntity<ApiResponse<TicketProductCreateResponse>> createTicket(
            @AuthenticationPrincipal AuthPrincipal authPrincipal,
            @PathVariable Long expoId,
            @Valid @RequestBody TicketProductCreateRequest request) {
        TicketProductCreateResponse response =
                ticketProductCreateService.ticketCreate(expoId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}
