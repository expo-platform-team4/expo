package com.expo.payment.service;

import com.expo.checkin.service.TicketIssueService;
import com.expo.common.config.TossConfirmResult;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.jwt.AuthPrincipal;
import com.expo.payment.dto.ConfirmTicketPaymentResponse;
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
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 토스 승인 성공 후 주문·재고·발권을 하나의 로컬 트랜잭션으로 확정한다. */
@Service
@RequiredArgsConstructor
class TicketPaymentCompletionService {

    private final TicketPaymentRepository ticketPaymentRepository;
    private final TicketPaymentHistoryRepository ticketPaymentHistoryRepository;
    private final TicketOrderRepository ticketOrderRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final TicketInventoryRepository ticketInventoryRepository;
    private final TicketOrderAccessVerifier ticketOrderAccessVerifier;
    private final TicketIssueService ticketIssueService;
    private final EntityManager entityManager;

    @Transactional
    public ConfirmTicketPaymentResponse complete(
            TicketPaymentConfirmationTarget target,
            TossConfirmResult result,
            AuthPrincipal principal) {
        TicketOrder order =
                ticketOrderRepository
                        .findByIdForUpdate(target.ticketOrderId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_ORDER_NOT_FOUND));
        ticketOrderAccessVerifier.verifyForPaymentFlow(order, principal);
        TicketPayment payment =
                ticketPaymentRepository
                        .findById(target.ticketPaymentId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == TicketPaymentStatus.DONE) {
            return toResponse(order, payment);
        }
        if (order.getStatus() != TicketOrderStatus.PENDING
                || payment.getRequestedAmount().compareTo(result.totalAmount()) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
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

        TicketPaymentStatus statusBeforeAttempt = payment.getStatus();
        payment.approve(result.paymentKey(), result.method(), result.totalAmount());
        order.markPaid();
        for (InventoryReservation reservation : reservations) {
            int updated =
                    ticketInventoryRepository.confirmReservedToSold(
                            reservation.getTicketProduct().getId(), reservation.getQuantity());
            if (updated != 1) {
                throw new BusinessException(ErrorCode.PAYMENT_RESERVATION_NOT_FOUND);
            }
            reservation.confirm();
        }
        ticketPaymentHistoryRepository.save(
                TicketPaymentHistory.record(
                        payment.getId(),
                        TicketPaymentEventType.APPROVE,
                        statusBeforeAttempt,
                        TicketPaymentStatus.DONE,
                        result.totalAmount(),
                        result.paymentKey(),
                        result.rawResponse()));

        // 발권은 MyBatis로 주문 상태를 다시 읽으므로 JPA 변경을 먼저 DB에 반영해야 한다.
        entityManager.flush();
        ticketIssueService.issue(order.getId());

        return toResponse(order, payment);
    }

    private ConfirmTicketPaymentResponse toResponse(TicketOrder order, TicketPayment payment) {
        return new ConfirmTicketPaymentResponse(
                payment.getId(),
                order.getOrderNumber(),
                payment.getPaymentKey(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getApprovedAmount(),
                payment.getApprovedAt());
    }
}
