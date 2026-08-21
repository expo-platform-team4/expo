package com.expo.refund.service;

import com.expo.refund.dto.GuestTicketRefundRequest;
import com.expo.refund.dto.TicketRefundResponse;
import com.expo.ticket.dto.GuestTicketOrderSearchSnapshot;
import com.expo.ticket.service.TicketOrderAccessVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 비회원 주문 인증 후 전체 환불 요청을 접수한다. */
@Service
@RequiredArgsConstructor
public class GuestTicketRefundRequestService {

    private final TicketOrderAccessVerifier ticketOrderAccessVerifier;
    private final TicketRefundRequestService ticketRefundRequestService;

    public TicketRefundResponse request(GuestTicketRefundRequest request) {
        GuestTicketOrderSearchSnapshot snapshot =
                ticketOrderAccessVerifier.verifyGuestOrderAccess(
                        request.orderNumber(), request.phoneNumber(), request.password());
        return ticketRefundRequestService.requestGuestRefund(
                snapshot.ticketOrderId(), request.reason());
    }
}
