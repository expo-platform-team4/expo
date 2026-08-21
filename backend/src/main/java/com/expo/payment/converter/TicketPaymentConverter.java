package com.expo.payment.converter;

import com.expo.payment.dto.TicketPaymentResponse;
import com.expo.payment.entity.TicketPayment;
import org.springframework.stereotype.Component;

@Component
public class TicketPaymentConverter {

    public TicketPaymentResponse toResponse(
            TicketPayment payment, String clientKey, String orderName) {
        return new TicketPaymentResponse(
                payment.getId(),
                clientKey,
                payment.getPgOrderId(),
                orderName,
                payment.getRequestedAmount());
    }
}
