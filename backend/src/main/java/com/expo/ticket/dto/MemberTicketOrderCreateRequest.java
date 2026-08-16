package com.expo.ticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MemberTicketOrderCreateRequest(
        @Valid @NotNull @Size(max = 4) List<TicketOrderItemRequest> items) {}
