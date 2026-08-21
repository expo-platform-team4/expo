package com.expo.payment.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.jwt.AuthPrincipal;
import com.expo.payment.converter.TicketPaymentConverter;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.entity.TicketPaymentStatus;
import com.expo.payment.repository.TicketPaymentRepository;
import com.expo.ticket.entity.InventoryReservation;
import com.expo.ticket.entity.InventoryReservationStatus;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrderStatus;
import com.expo.ticket.repository.InventoryReservationRepository;
import com.expo.ticket.repository.TicketOrderRepository;
import com.expo.ticket.service.TicketOrderAccessVerifier;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 토스 승인 호출 전에 주문·금액·재고 예약을 짧은 트랜잭션으로 검증한다. */
@Service
@RequiredArgsConstructor
class TicketPaymentConfirmationPreparationService {

    private static final Duration CONFIRMATION_LEASE_DURATION = Duration.ofMinutes(2);

    private final TicketPaymentRepository ticketPaymentRepository;
    private final TicketOrderRepository ticketOrderRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final TicketOrderAccessVerifier ticketOrderAccessVerifier;
    private final TicketPaymentConverter ticketPaymentConverter;

    @Transactional
    public TicketPaymentConfirmationTarget prepare(
            String pgOrderId, BigDecimal amount, AuthPrincipal principal) {
        TicketPayment payment =
                ticketPaymentRepository
                        .findByPgOrderId(pgOrderId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        TicketOrder order =
                ticketOrderRepository
                        .findByIdForUpdate(payment.getTicketOrderId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_ORDER_NOT_FOUND));
        ticketOrderAccessVerifier.verifyForPaymentFlow(order, principal);

        if (payment.getStatus() == TicketPaymentStatus.DONE) {
            return new TicketPaymentConfirmationTarget(
                    order.getId(),
                    payment.getId(),
                    ticketPaymentConverter.toConfirmResponse(order, payment));
        }
        if (payment.getStatus() == TicketPaymentStatus.CANCELED
                || order.getStatus() != TicketOrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_NOT_PENDING);
        }
        if (payment.getRequestedAmount().compareTo(amount) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        validateAndExtendActiveReservations(order.getId());
        return new TicketPaymentConfirmationTarget(order.getId(), payment.getId(), null);
    }

    private void validateAndExtendActiveReservations(Long ticketOrderId) {
        List<InventoryReservation> reservations =
                inventoryReservationRepository.findAllByTicketOrderIdForUpdate(ticketOrderId);
        if (reservations.isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_RESERVATION_NOT_FOUND);
        }
        Instant confirmationLeaseExpiresAt = Instant.now().plus(CONFIRMATION_LEASE_DURATION);
        for (InventoryReservation reservation : reservations) {
            if (reservation.getStatus() != InventoryReservationStatus.ACTIVE
                    || Instant.now().isAfter(reservation.getExpiresAt())) {
                throw new BusinessException(ErrorCode.PAYMENT_ORDER_EXPIRED);
            }
            reservation.extendExpiration(confirmationLeaseExpiresAt);
        }
    }
}
