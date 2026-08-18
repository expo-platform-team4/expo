package com.expo.ticket.dto;

public record GuestTicketOrderReponse(
        TicketOrderResponse ticketOrderResponse, String guestName, int guestAge) {}
