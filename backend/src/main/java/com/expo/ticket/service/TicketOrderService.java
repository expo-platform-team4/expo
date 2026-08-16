package com.expo.ticket.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.ticket.converter.TicketOrderConverter;
import com.expo.ticket.dto.MemberTicketOrderCreateRequest;
import com.expo.ticket.dto.TicketOrderResponse;
import com.expo.ticket.entity.InventoryReservation;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrderItem;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.repository.InventoryReservationRepository;
import com.expo.ticket.repository.TicketInventoryRepository;
import com.expo.ticket.repository.TicketOrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketOrderService {

    private static final BigDecimal BOOKING_FEE_RATE = new BigDecimal("0.03");
    private static final int MAX_TOTAL_QUANTITY = 4;

    private final InventoryReservationRepository inventoryReservationRepository;
    private final TicketOrderConverter ticketOrderConverter;
    private final TicketOrderItemCreatorService ticketOrderItemCreatorService;
    private final TicketOrderRepository ticketOrderRepository;
    private final TicketInventoryRepository ticketInventoryRepository;

    @Transactional
    public TicketOrderResponse memberCreateOrder(
            Long memberId, MemberTicketOrderCreateRequest request) {
        List<TicketOrderItem> orderItems =
                ticketOrderItemCreatorService.creatorOrderItems(request.items());

        int totalQuantity = 0;
        BigDecimal ticketSubtotalAmount = BigDecimal.ZERO;

        for (TicketOrderItem orderItem : orderItems) {
            totalQuantity += orderItem.getQuantity();
            ticketSubtotalAmount = ticketSubtotalAmount.add(orderItem.getItemSubtotalAmount());
        }

        if (totalQuantity < 1 || totalQuantity > MAX_TOTAL_QUANTITY) {
            throw new BusinessException(ErrorCode.TICKET_TOTAL_QUANTITY_EXCEEDED);
        }

        BigDecimal bookingFeeAmount =
                ticketSubtotalAmount.multiply(BOOKING_FEE_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = ticketSubtotalAmount.add(bookingFeeAmount);

        String orderNumber = "TICKET-" + UUID.randomUUID();

        TicketOrder order =
                TicketOrder.createMemberOrder(
                        orderNumber,
                        memberId,
                        ticketSubtotalAmount,
                        BOOKING_FEE_RATE,
                        bookingFeeAmount,
                        totalAmount,
                        totalQuantity);

        for (TicketOrderItem orderItem : orderItems) {
            order.addItem(orderItem);
        }

        TicketOrder saveOrder = ticketOrderRepository.save(order);

        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(10));

        for (TicketOrderItem orderItem : orderItems) {
            TicketProduct product = orderItem.getTicketProduct();

            int update =
                    ticketInventoryRepository.reserveIfAvailable(
                            product.getId(), orderItem.getQuantity());
            if (update == 0) {
                throw new BusinessException(ErrorCode.TICKET_INSUFFICIENT_STOCK);
            }

            InventoryReservation reservation =
                    InventoryReservation.builder()
                            .ticketProduct(product)
                            .ticketOrder(saveOrder)
                            .quantity(orderItem.getQuantity())
                            .expiresAt(expiresAt)
                            .build();

            inventoryReservationRepository.save(reservation);
        }

        return ticketOrderConverter.toTicketOrderResponse(saveOrder, expiresAt);
    }
}
