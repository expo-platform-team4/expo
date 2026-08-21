package com.expo.payment.service;

import com.expo.common.config.TossApiException;
import com.expo.common.config.TossConfirmResult;
import com.expo.common.config.TossPaymentClient;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.jwt.AuthPrincipal;
import com.expo.payment.dto.ConfirmTicketPaymentRequest;
import com.expo.payment.dto.ConfirmTicketPaymentResponse;
import com.expo.payment.entity.TicketPayment;
import com.expo.payment.repository.TicketPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 티켓 결제 승인 요청을 토스에 전달하고 로컬 주문을 확정한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketPaymentConfirmationService {

    private final TicketPaymentConfirmationPreparationService preparationService;
    private final TicketPaymentCompletionService completionService;
    private final TicketPaymentFailureService ticketPaymentFailureService;
    private final TicketPaymentRepository ticketPaymentRepository;
    private final TossPaymentClient tossPaymentClient;

    public ConfirmTicketPaymentResponse confirm(
            ConfirmTicketPaymentRequest request, AuthPrincipal principal) {
        TicketPaymentConfirmationTarget target =
                preparationService.prepare(request.orderId(), request.amount(), principal);
        if (target.alreadyApprovedResponse() != null) {
            return target.alreadyApprovedResponse();
        }

        TicketPayment payment =
                ticketPaymentRepository
                        .findById(target.ticketPaymentId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        try {
            TossConfirmResult result =
                    tossPaymentClient.confirmPayment(
                            request.paymentKey(),
                            request.orderId(),
                            request.amount(),
                            payment.getIdempotencyKey());
            return completionService.complete(target, result, principal);
        } catch (TossApiException e) {
            ticketPaymentFailureService.recordFailure(
                    target.ticketPaymentId(), e.getCode(), e.getRawResponse());
            log.warn(
                    "티켓 결제 승인 실패. ticketPaymentId={}, pgOrderId={}, tossCode={}",
                    target.ticketPaymentId(),
                    request.orderId(),
                    e.getCode(),
                    e);
            throw new BusinessException(ErrorCode.PAYMENT_APPROVAL_FAILED);
        }
    }
}
