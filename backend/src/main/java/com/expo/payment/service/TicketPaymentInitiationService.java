package com.expo.payment.service;

import com.expo.common.config.TossPaymentClient;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.payment.converter.TicketPaymentConverter;
import com.expo.payment.dto.TicketPaymentResponse;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.repository.TicketPaymentRepository;
import com.expo.ticket.entity.InventoryReservation;
import com.expo.ticket.entity.InventoryReservationStatus;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrderStatus;
import com.expo.ticket.repository.InventoryReservationRepository;
import com.expo.ticket.repository.TicketOrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketPaymentInitiationService {

    private final TicketOrderRepository ticketOrderRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final TicketPaymentRepository ticketPaymentRepository;
    private final TicketPaymentConverter ticketPaymentConverter;
    private final TossPaymentClient tossPaymentClient;

    @Transactional
    public TicketPaymentResponse initiate(String orderNumber) {
        TicketOrder order =
                ticketOrderRepository
                        .findByOrderNumberForUpdate(orderNumber)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER_NUMBER));

        if (order.getStatus() != TicketOrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_NOT_PENDING);
        }

        List<InventoryReservation> reservations =
                inventoryReservationRepository.findAllByTicketOrderId(order.getId());

        if (reservations.isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_RESERVATION_NOT_FOUND);
        }

        for (InventoryReservation reservation : reservations) {
            if (reservation.getStatus() != InventoryReservationStatus.ACTIVE
                    || Instant.now().isAfter(reservation.getExpiresAt())) {
                throw new BusinessException(ErrorCode.PAYMENT_ORDER_EXPIRED);
            }
        }

        TicketPayment payment =
                ticketPaymentRepository
                        .findByTicketOrderId(order.getId())
                        .orElseGet(
                                () ->
                                        ticketPaymentRepository.save(
                                                TicketPayment.create(
                                                        order.getId(),
                                                        order.getOrderNumber() + "-P1",
                                                        order.getTotalAmount(),
                                                        order.getTicketSubtotalAmount(),
                                                        order.getBookingFeeAmount(),
                                                        UUID.randomUUID().toString())));

        return ticketPaymentConverter.toResponse(
                payment, tossPaymentClient.getClientKey(), "티켓 " + order.getTotalQuantity() + "매");
    }
}
