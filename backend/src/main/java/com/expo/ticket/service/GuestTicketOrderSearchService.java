package com.expo.ticket.service;

import com.expo.ticket.dto.GuestTicketSearchRequest;
import com.expo.ticket.dto.GuestTicketSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuestTicketOrderSearchService {

    private final TicketOrderAccessVerifier ticketOrderAccessVerifier;

    public GuestTicketSearchResponse guestTicketSearch(GuestTicketSearchRequest request) {
        return ticketOrderAccessVerifier
                .verifyGuestOrderAccess(
                        request.orderNumber(), request.phoneNumber(), request.password())
                .response();
    }
}
