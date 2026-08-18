package com.expo.ticket.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.ticket.dto.GuestTicketOrderSearchSnapshot;
import com.expo.ticket.dto.GuestTicketSearchRequest;
import com.expo.ticket.dto.GuestTicketSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuestTicketOrderSearchService {

    private final PasswordEncoder passwordEncoder;
    private final GuestTicketOrderSnapshotService guestTicketOrderSnapshotService;
    private final GuestTicketOrderAttemptService guestTicketOrderAttemptService;

    public GuestTicketSearchResponse guestTicketSearch(GuestTicketSearchRequest request) {
        GuestTicketOrderSearchSnapshot snapshot =
                guestTicketOrderSnapshotService.findByOrderNumber(request.orderNumber());

        if (!snapshot.phoneNumber().equals(request.phoneNumber())
                || !passwordEncoder.matches(request.password(), snapshot.lookupPasswordHash())) {
            guestTicketOrderAttemptService.recordFailure(snapshot.ticketOrderId());
            throw new BusinessException(ErrorCode.GUEST_ORDER_LOOKUP_FAILED);
        }

        guestTicketOrderAttemptService.resetFailures(snapshot.ticketOrderId());

        return snapshot.response();
    }
}
