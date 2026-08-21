package com.expo.refund.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.jwt.AuthPrincipal;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.entity.TicketPaymentStatus;
import com.expo.payment.repository.TicketPaymentRepository;
import com.expo.payment.service.TicketOrderAccessVerifier;
import com.expo.refund.converter.TicketRefundConverter;
import com.expo.refund.dto.TicketRefundResponse;
import com.expo.refund.entity.TicketRefund;
import com.expo.refund.repository.TicketRefundRepository;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrderStatus;
import com.expo.ticket.repository.TicketOrderRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public TicketRefundResponse requestMemberRefund(
            Long orderId, String reason, AuthPrincipal principal) {
        TicketOrder order =
                ticketOrderRepository
                        .findByIdForUpdate(orderId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_ORDER_ACCESS_DENIED));
        ticketOrderAccessVerifier.verifyMemberOrderAccess(order, principal);
        validateRefundableOrder(order);

        if (ticketRefundRepository.findByTicketOrderId(order.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_REQUESTED);
        }
        TicketPayment payment =
                ticketPaymentRepository
                        .findByTicketOrderId(order.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_PAYMENT_NOT_COMPLETED));
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
        return ticketRefundConverter.toResponse(refund);
    }

    private void validateRefundableOrder(TicketOrder order) {
        if (order.getStatus() != TicketOrderStatus.PAID) {
            throw new BusinessException(ErrorCode.REFUND_ORDER_NOT_PAID);
        }
    }
}
