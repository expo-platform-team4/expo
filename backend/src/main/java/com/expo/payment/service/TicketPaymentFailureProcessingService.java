package com.expo.payment.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.jwt.AuthPrincipal;
import com.expo.payment.dto.FailTicketPaymentResponse;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.entity.TicketPaymentEventType;
import com.expo.payment.entity.TicketPaymentHistory;
import com.expo.payment.entity.TicketPaymentStatus;
import com.expo.payment.repository.TicketPaymentHistoryRepository;
import com.expo.payment.repository.TicketPaymentRepository;
import com.expo.ticket.entity.InventoryReservation;
import com.expo.ticket.entity.InventoryReservationStatus;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrderStatus;
import com.expo.ticket.repository.InventoryReservationRepository;
import com.expo.ticket.repository.TicketInventoryRepository;
import com.expo.ticket.repository.TicketOrderRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 결제 실패를 기록하고 주문의 임시 확보 재고를 반환한다. */
@Service
@RequiredArgsConstructor
public class TicketPaymentFailureProcessingService {

    private final TicketPaymentRepository ticketPaymentRepository;
    private final TicketPaymentHistoryRepository ticketPaymentHistoryRepository;
    private final TicketOrderRepository ticketOrderRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final TicketInventoryRepository ticketInventoryRepository;
    private final TicketOrderAccessVerifier ticketOrderAccessVerifier;

    @Transactional
    public FailTicketPaymentResponse process(
            String pgOrderId, String failureCode, AuthPrincipal principal) {
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
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_APPROVED);
        }
        if (payment.getStatus() == TicketPaymentStatus.FAILED) {
            return toResponse(payment, order);
        }
        if (payment.getStatus() == TicketPaymentStatus.CANCELED
                || order.getStatus() != TicketOrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_NOT_PENDING);
        }

        List<InventoryReservation> reservations =
                inventoryReservationRepository.findAllByTicketOrderId(order.getId());
        if (reservations.isEmpty()
                || reservations.stream()
                        .anyMatch(
                                reservation ->
                                        reservation.getStatus()
                                                != InventoryReservationStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.PAYMENT_RESERVATION_NOT_FOUND);
        }

        TicketPaymentStatus statusBeforeFailure = payment.getStatus();
        for (InventoryReservation reservation : reservations) {
            int updated =
                    ticketInventoryRepository.releaseReserved(
                            reservation.getTicketProduct().getId(), reservation.getQuantity());
            if (updated != 1) {
                throw new BusinessException(ErrorCode.PAYMENT_RESERVATION_NOT_FOUND);
            }
            reservation.release();
        }
        payment.fail(failureCode);
        order.markPaymentFailed();
        ticketPaymentHistoryRepository.save(
                TicketPaymentHistory.record(
                        payment.getId(),
                        TicketPaymentEventType.FAIL,
                        statusBeforeFailure,
                        TicketPaymentStatus.FAILED,
                        null,
                        null,
                        null));
        return toResponse(payment, order);
    }

    private FailTicketPaymentResponse toResponse(TicketPayment payment, TicketOrder order) {
        return new FailTicketPaymentResponse(
                payment.getId(), order.getOrderNumber(), payment.getStatus(), order.getStatus());
    }
}
