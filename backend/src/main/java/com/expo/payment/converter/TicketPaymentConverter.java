package com.expo.payment.converter;

import com.expo.payment.dto.ConfirmTicketPaymentResponse;
import com.expo.payment.dto.FailTicketPaymentResponse;
import com.expo.payment.dto.TicketPaymentResponse;
import com.expo.payment.dto.TicketPaymentStatusResponse;
import com.expo.payment.entity.TicketPayment;
import com.expo.ticket.entity.TicketOrder;
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

    public ConfirmTicketPaymentResponse toConfirmResponse(
            TicketOrder order, TicketPayment payment) {
        return new ConfirmTicketPaymentResponse(
                payment.getId(),
                order.getOrderNumber(),
                payment.getPaymentKey(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getApprovedAmount(),
                payment.getApprovedAt());
    }

    public FailTicketPaymentResponse toFailureResponse(TicketOrder order, TicketPayment payment) {
        return new FailTicketPaymentResponse(
                payment.getId(), order.getOrderNumber(), payment.getStatus(), order.getStatus());
    }

    public TicketPaymentStatusResponse toStatusResponse(TicketOrder order, TicketPayment payment) {
        return new TicketPaymentStatusResponse(
                order.getOrderNumber(),
                order.getStatus(),
                payment.getId(),
                payment.getStatus(),
                payment.getRequestedAmount(),
                payment.getApprovedAt(),
                payment.getLastFailureCode());
    }

    public TicketPaymentStatusResponse toStatusResponse(TicketOrder order) {
        return new TicketPaymentStatusResponse(
                order.getOrderNumber(),
                order.getStatus(),
                null,
                null,
                order.getTotalAmount(),
                null,
                null);
    }
}
