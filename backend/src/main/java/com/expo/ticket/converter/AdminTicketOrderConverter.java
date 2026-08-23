package com.expo.ticket.converter;

import com.expo.ticket.dto.AdminTicketOrderDetailResponse;
import com.expo.ticket.dto.AdminTicketOrderResponse;
import com.expo.ticket.entity.TicketOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 관리자 티켓 주문 조회용 Entity-DTO 변환. */
@Component
@RequiredArgsConstructor
public class AdminTicketOrderConverter {

    private final TicketOrderItemConverter ticketOrderItemConverter;

    public AdminTicketOrderResponse toResponse(TicketOrder order) {
        return new AdminTicketOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrdererType(),
                order.getMemberUserId(),
                order.getStatus(),
                order.getTotalQuantity(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }

    public AdminTicketOrderDetailResponse toDetailResponse(TicketOrder order) {
        return new AdminTicketOrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrdererType(),
                order.getMemberUserId(),
                order.getStatus(),
                order.getTicketSubtotalAmount(),
                order.getBookingFeeAmount(),
                order.getTotalAmount(),
                order.getPaidAt(),
                order.getCanceledAt(),
                order.getCreatedAt(),
                order.getItems().stream()
                        .map(ticketOrderItemConverter::toTicketOrderItemResponse)
                        .toList());
    }
}
