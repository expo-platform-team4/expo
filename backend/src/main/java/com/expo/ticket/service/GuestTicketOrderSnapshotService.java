package com.expo.ticket.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.ticket.converter.TicketOrderConverter;
import com.expo.ticket.dto.GuestTicketOrderSearchSnapshot;
import com.expo.ticket.entity.GuestOrder;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.repository.GuestOrderInfoRepository;
import com.expo.ticket.repository.TicketOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestTicketOrderSnapshotService {

    private final TicketOrderRepository ticketOrderRepository;
    private final GuestOrderInfoRepository guestOrderInfoRepository;
    private final TicketOrderConverter ticketOrderConverter;

    @Transactional(readOnly = true)
    public GuestTicketOrderSearchSnapshot findByOrderNumber(String orderNumber) {
        TicketOrder order =
                ticketOrderRepository
                        .findByOrderNumber(orderNumber)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.GUEST_ORDER_LOOKUP_FAILED));
        GuestOrder guestOrder =
                guestOrderInfoRepository
                        .findById(order.getId())
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.GUEST_ORDER_LOOKUP_FAILED));

        guestOrder.validateNotLocked();

        return new GuestTicketOrderSearchSnapshot(
                order.getId(),
                guestOrder.getPhoneNumber(),
                guestOrder.getLookupPasswordHash(),
                ticketOrderConverter.toGuestTicketSearchResponse(order));
    }
}
