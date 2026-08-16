package com.expo.ticket.dto;

import java.util.List;

public record GuestTicketOrderRequest(
        List<TicketOrderItemRequest> items,
        String name,
        int phoneNumber,
        int age,
        String password) {}
