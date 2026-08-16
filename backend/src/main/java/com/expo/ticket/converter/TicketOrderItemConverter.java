package com.expo.ticket.converter;

import com.expo.ticket.dto.TicketOrderItemResponse;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketProduct;
import org.springframework.stereotype.Component;

@Component
public class TicketOrderConverter {

    public TicketOrderItemResponse toTicketOrderItemResponse(
            TicketOrder order, TicketProduct product) {

        return new TicketOrderItemResponse(product.getId(), product.getName(), order.get);
    }
}
