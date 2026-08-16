package com.expo.ticket.converter;

import com.expo.ticket.dto.GuestTicketOrderReponse;
import com.expo.ticket.dto.TicketOrderResponse;
import com.expo.ticket.entity.GuestOrder;
import com.expo.ticket.entity.TicketOrder;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketOrderConverter {

    private final TicketOrderItemConverter ticketOrderItemConverter;

    public TicketOrderResponse toTicketOrderResponse(TicketOrder order, Instant expiresAt) {
        return new TicketOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getItems().stream()
                        .map(ticketOrderItemConverter::toTicketOrderItemResponse)
                        .toList(),
                order.getTicketSubtotalAmount(),
                order.getBookingFeeAmount(),
                order.getTotalAmount(),
                expiresAt,
                order.getCreatedAt());
    }

    public GuestTicketOrderReponse toGuestTicketOrderResponse(
            TicketOrder order, Instant expiresAt, GuestOrder guestOrder) {
        TicketOrderResponse base = toTicketOrderResponse(order, expiresAt);

        return new GuestTicketOrderReponse(base, guestOrder.getGuestName(), guestOrder.getAge());
    }
}
