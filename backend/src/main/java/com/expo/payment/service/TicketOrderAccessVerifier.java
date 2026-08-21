package com.expo.payment.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.jwt.AuthPrincipal;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrdererType;
import org.springframework.stereotype.Component;

/** 결제 진행 중 회원 주문의 소유자를 검증한다. 게스트는 예측 불가능한 주문 번호로 이어지는 흐름을 사용한다. */
@Component
public class TicketOrderAccessVerifier {

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
}
