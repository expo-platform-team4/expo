package com.expo.ticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MemberTicketOrderRequest(
        @Valid @NotEmpty @Size(max = 4) List<TicketOrderItemRequest> items) {}
