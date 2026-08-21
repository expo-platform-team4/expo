package com.expo.payment.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.jwt.AuthPrincipal;
import com.expo.payment.converter.TicketPaymentConverter;
import com.expo.payment.dto.TicketPaymentStatusResponse;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.repository.TicketPaymentRepository;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.repository.TicketOrderRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 티켓 주문의 최신 결제 상태를 조회한다. */
@Service
@RequiredArgsConstructor
public class TicketPaymentStatusService {

    private final TicketOrderRepository ticketOrderRepository;
    private final TicketPaymentRepository ticketPaymentRepository;
    private final TicketOrderAccessVerifier ticketOrderAccessVerifier;
    private final TicketPaymentConverter ticketPaymentConverter;

    @Transactional(readOnly = true)
    public TicketPaymentStatusResponse getStatus(String orderNumber, AuthPrincipal principal) {
        TicketOrder order =
                ticketOrderRepository
                        .findByOrderNumber(orderNumber)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.TICKET_ORDER_ACCESS_DENIED));
        ticketOrderAccessVerifier.verifyForPaymentFlow(order, principal);
        Optional<TicketPayment> payment =
                ticketPaymentRepository.findByTicketOrderId(order.getId());

        return payment.map(found -> ticketPaymentConverter.toStatusResponse(order, found))
                .orElseGet(() -> ticketPaymentConverter.toStatusResponse(order));
    }
}
