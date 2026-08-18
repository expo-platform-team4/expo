package com.expo.ticket.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.ticket.converter.TicketOrderConverter;
import com.expo.ticket.dto.GuestTicketSearchRequest;
import com.expo.ticket.dto.GuestTicketSearchResponse;
import com.expo.ticket.entity.GuestOrder;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.repository.GuestOrderInfoRepository;
import com.expo.ticket.repository.TicketOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestTicketOrderSearchService {

    private final TicketOrderRepository ticketOrderRepository;
    private final GuestOrderInfoRepository guestOrderInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final TicketOrderConverter ticketOrderConverter;

    @Transactional
    public GuestTicketSearchResponse guestTicketSearch(GuestTicketSearchRequest request) {
        TicketOrder order =
                ticketOrderRepository
                        .findByOrderNumber(request.orderNumber())
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.GUEST_ORDER_LOOKUP_FAILED));

        GuestOrder guestOrder =
                guestOrderInfoRepository
                        .findById(order.getId())
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.GUEST_ORDER_LOOKUP_FAILED));

        guestOrder.validateNotLocked();

        if (!guestOrder.getPhoneNumber().equals(request.phoneNumber())
                || !passwordEncoder.matches(
                        request.password(), guestOrder.getLookupPasswordHash())) {
            guestOrder.recordFailedAttempt();
            throw new BusinessException(ErrorCode.GUEST_ORDER_LOOKUP_FAILED);
        }

        guestOrder.resetFailedAttempts();

        return ticketOrderConverter.toGuestTicketSearchResponse(order);
    }
}
