package com.expo.payment.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.jwt.AuthPrincipal;
import com.expo.ticket.dto.GuestTicketOrderSearchSnapshot;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrdererType;
import com.expo.ticket.service.GuestTicketOrderAttemptService;
import com.expo.ticket.service.GuestTicketOrderSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 결제 진행 중 회원 주문의 소유자를 검증한다. 게스트는 예측 불가능한 주문 번호로 이어지는 흐름을 사용한다. */
@Component
@RequiredArgsConstructor
public class TicketOrderAccessVerifier {

    private final PasswordEncoder passwordEncoder;
    private final GuestTicketOrderSnapshotService guestTicketOrderSnapshotService;
    private final GuestTicketOrderAttemptService guestTicketOrderAttemptService;

    public void verifyForPaymentFlow(TicketOrder order, AuthPrincipal principal) {
        if (order.getOrdererType() != TicketOrdererType.MEMBER) {
            return;
        }
        if (principal == null || !order.getMemberUserId().equals(principal.getMemberId())) {
            throw new BusinessException(ErrorCode.TICKET_ORDER_ACCESS_DENIED);
        }
    }

    /** 회원 전용 기능에서 회원 주문의 소유자를 검증한다. */
    public void verifyMemberOrderAccess(TicketOrder order, AuthPrincipal principal) {
        if (order.getOrdererType() != TicketOrdererType.MEMBER
                || principal == null
                || !order.getMemberUserId().equals(principal.getMemberId())) {
            throw new BusinessException(ErrorCode.TICKET_ORDER_ACCESS_DENIED);
        }
    }

    /** 비회원 주문 비밀번호를 확인하고 성공 시 주문 식별자를 반환한다. */
    public Long verifyGuestOrderAccess(String orderNumber, String phoneNumber, String password) {
        GuestTicketOrderSearchSnapshot snapshot =
                guestTicketOrderSnapshotService.findByOrderNumber(orderNumber);
        if (!snapshot.phoneNumber().equals(phoneNumber)
                || !passwordEncoder.matches(password, snapshot.lookupPasswordHash())) {
            guestTicketOrderAttemptService.recordFailure(snapshot.ticketOrderId());
            throw new BusinessException(ErrorCode.GUEST_ORDER_LOOKUP_FAILED);
        }
        guestTicketOrderAttemptService.resetFailures(snapshot.ticketOrderId());
        return snapshot.ticketOrderId();
    }
}
