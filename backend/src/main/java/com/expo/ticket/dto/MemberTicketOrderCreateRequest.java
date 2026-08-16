package com.expo.ticket.dto;

import jakarta.validation.Valid;
import java.util.List;

public record MemberTicketOrderCreateRequest(@Valid List<TicketOrderItemRequest> items) {}
