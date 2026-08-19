package com.expo.ticket.converter;

import com.expo.ticket.dto.TicketOrderItemResponse;
import com.expo.ticket.entity.TicketOrderItem;
import com.expo.ticket.entity.TicketProduct;
import org.springframework.stereotype.Component;

@Component
public class TicketOrderItemConverter {

    public TicketOrderItemResponse toTicketOrderItemResponse(TicketOrderItem orderItem) {

        TicketProduct product = orderItem.getTicketProduct();

        return new TicketOrderItemResponse(
                product.getId(),
                product.getName(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity(),
                orderItem.getItemSubtotalAmount());
    }
}
