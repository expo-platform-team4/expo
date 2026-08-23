package com.expo.refund.service;

import com.expo.checkin.repository.IssuedTicketRepository;
import com.expo.expo.entity.Expo;
import com.expo.expo.repository.ExpoRepository;
import com.expo.refund.dto.GuestTicketRefundEligibilityRequest;
import com.expo.refund.dto.TicketRefundEligibilityResponse;
import com.expo.refund.dto.TicketRefundIneligibilityReason;
import com.expo.ticket.dto.GuestTicketOrderSearchSnapshot;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrderStatus;
import com.expo.ticket.repository.TicketOrderRepository;
import com.expo.ticket.service.TicketOrderAccessVerifier;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 비회원 주문의 전체 환불 가능 여부를 조회한다. */
@Service
@RequiredArgsConstructor
public class GuestTicketRefundEligibilityService {

    private static final Duration REFUND_CUTOFF = Duration.ofDays(3);

    private final TicketOrderAccessVerifier ticketOrderAccessVerifier;
    private final TicketOrderRepository ticketOrderRepository;
    private final ExpoRepository expoRepository;
    private final IssuedTicketRepository issuedTicketRepository;

    @Transactional(readOnly = true)
    public TicketRefundEligibilityResponse check(GuestTicketRefundEligibilityRequest request) {
        GuestTicketOrderSearchSnapshot snapshot =
                ticketOrderAccessVerifier.verifyGuestOrderAccess(
                        request.orderNumber(), request.phoneNumber(), request.password());
        TicketOrder order = ticketOrderRepository.findById(snapshot.ticketOrderId()).orElseThrow();
        if (order.getStatus() != TicketOrderStatus.PAID) {
            return ineligible(order, TicketRefundIneligibilityReason.ORDER_NOT_PAID);
        }
        Long expoId = order.getItems().getFirst().getTicketProduct().getExpoId();
        Expo expo = expoRepository.findById(expoId).orElseThrow();
        if (!expo.getEventStartAt().isAfter(Instant.now().plus(REFUND_CUTOFF))) {
            return ineligible(
                    order, TicketRefundIneligibilityReason.EVENT_STARTS_WITHIN_THREE_DAYS);
        }
        if (issuedTicketRepository.existsCheckedInByTicketOrderId(order.getId())) {
            return ineligible(order, TicketRefundIneligibilityReason.TICKET_ALREADY_CHECKED_IN);
        }
        return new TicketRefundEligibilityResponse(true, null, order.getTotalAmount());
    }

    private TicketRefundEligibilityResponse ineligible(
            TicketOrder order, TicketRefundIneligibilityReason reason) {
        return new TicketRefundEligibilityResponse(false, reason, order.getTotalAmount());
    }
}
