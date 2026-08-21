package com.expo.ticket.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.jwt.AuthPrincipal;
import com.expo.ticket.dto.GuestTicketOrderSearchSnapshot;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrdererType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 회원·비회원 티켓 주문의 접근 권한을 공통 정책으로 검증한다. */
@Component
@RequiredArgsConstructor
public class TicketOrderAccessVerifier {

    private final PasswordEncoder passwordEncoder;
    private final GuestTicketOrderSnapshotService guestTicketOrderSnapshotService;
    private final GuestTicketOrderAttemptService guestTicketOrderAttemptService;

    /** 결제 진행 흐름에서 회원 주문의 소유자를 검증한다. */
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

    /** 비회원 주문의 휴대폰 번호·비밀번호를 검증하고 인증에 사용한 주문 snapshot을 반환한다. */
    public GuestTicketOrderSearchSnapshot verifyGuestOrderAccess(
            String orderNumber, String phoneNumber, String password) {
        GuestTicketOrderSearchSnapshot snapshot =
                guestTicketOrderSnapshotService.findByOrderNumber(orderNumber);
        boolean phoneMatches = snapshot.phoneNumber().equals(phoneNumber);
        boolean passwordMatches = passwordEncoder.matches(password, snapshot.lookupPasswordHash());
        if (!phoneMatches || !passwordMatches) {
            guestTicketOrderAttemptService.recordFailure(snapshot.ticketOrderId());
            throw new BusinessException(ErrorCode.GUEST_ORDER_LOOKUP_FAILED);
        }
        guestTicketOrderAttemptService.resetFailures(snapshot.ticketOrderId());
        return snapshot;
    }
}
