package com.expo.ticket.converter;

import com.expo.ticket.dto.TicketOrderResponse;
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
}
