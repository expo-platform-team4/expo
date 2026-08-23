package com.expo.refund.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.jwt.AuthPrincipal;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.entity.TicketPaymentStatus;
import com.expo.payment.repository.TicketPaymentRepository;
import com.expo.refund.converter.TicketRefundConverter;
import com.expo.refund.dto.TicketRefundResponse;
import com.expo.refund.entity.TicketRefund;
import com.expo.refund.event.TicketRefundRequestedEvent;
import com.expo.refund.repository.TicketRefundRepository;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrderStatus;
import com.expo.ticket.entity.TicketOrdererType;
import com.expo.ticket.repository.TicketOrderRepository;
import com.expo.ticket.service.TicketOrderAccessVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원 티켓 주문의 전체 환불 요청을 접수한다. */
@Service
@RequiredArgsConstructor
public class TicketRefundRequestService {

    private final TicketOrderRepository ticketOrderRepository;
    private final TicketPaymentRepository ticketPaymentRepository;
    private final TicketRefundRepository ticketRefundRepository;
    private final TicketOrderAccessVerifier ticketOrderAccessVerifier;
    private final TicketRefundConverter ticketRefundConverter;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public TicketRefundResponse requestMemberRefund(
            Long orderId, String reason, AuthPrincipal principal) {
        TicketOrder order =
                ticketOrderRepository
                        .findByIdForUpdate(orderId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.TICKET_ORDER_ACCESS_DENIED));
        ticketOrderAccessVerifier.verifyMemberOrderAccess(order, principal);
        return createRefundRequest(order, reason);
    }

    @Transactional
    public TicketRefundResponse requestGuestRefund(Long orderId, String reason) {
        TicketOrder order =
                ticketOrderRepository
                        .findByIdForUpdate(orderId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.GUEST_ORDER_LOOKUP_FAILED));
        if (order.getOrdererType() != TicketOrdererType.GUEST) {
            throw new BusinessException(ErrorCode.GUEST_ORDER_LOOKUP_FAILED);
        }
        return createRefundRequest(order, reason);
    }

    private TicketRefundResponse createRefundRequest(TicketOrder order, String reason) {
        validateRefundableOrder(order);

        if (ticketRefundRepository.findByTicketOrderId(order.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_REQUESTED);
        }
        TicketPayment payment =
                ticketPaymentRepository
                        .findByTicketOrderId(order.getId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.REFUND_PAYMENT_NOT_COMPLETED));
        if (payment.getStatus() != TicketPaymentStatus.DONE) {
            throw new BusinessException(ErrorCode.REFUND_PAYMENT_NOT_COMPLETED);
        }

        TicketRefund refund =
                ticketRefundRepository.save(
                        TicketRefund.create(
                                order.getId(),
                                payment.getId(),
                                order.getTicketSubtotalAmount(),
                                order.getBookingFeeAmount(),
                                reason));
        applicationEventPublisher.publishEvent(new TicketRefundRequestedEvent(refund.getId()));
        return ticketRefundConverter.toResponse(refund);
    }

    private void validateRefundableOrder(TicketOrder order) {
        if (order.getStatus() != TicketOrderStatus.PAID) {
            throw new BusinessException(ErrorCode.REFUND_ORDER_NOT_PAID);
        }
    }
}
