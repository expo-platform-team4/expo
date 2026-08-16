package com.expo.ticket.converter;

import com.expo.ticket.dto.TicketOrderResponse;
import com.expo.ticket.entity.InventoryReservation;
import com.expo.ticket.entity.TicketOrder;
import org.springframework.stereotype.Component;

@Component
public class TicketOrderConvertor {

    public TicketOrderResponse memberTickerOrder(
        TicketOrder order,
        InventoryReservation inventoryReservation
    ) {


        return new TicketOrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getStatus(),
            order.getItems(),
            order.getTicketSubtotalAmount(),
            order.getBookingFeeAmount(),
            order.getTotalAmount(),
            inventoryReservation.getExpiresAt(),
            order.getCreatedAt()
        );
    }
}
