package com.expo.refund.service;

import com.expo.checkin.repository.IssuedTicketRepository;
import com.expo.expo.entity.Expo;
import com.expo.expo.repository.ExpoRepository;
import com.expo.refund.dto.GuestTicketRefundEligibilityRequest;
import com.expo.refund.dto.TicketRefundEligibilityResponse;
import com.expo.refund.dto.TicketRefundIneligibilityReason;
import com.expo.ticket.dto.GuestTicketOrderSearchSnapshot;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrderStatus;
import com.expo.ticket.repository.TicketOrderRepository;
import com.expo.ticket.service.TicketOrderAccessVerifier;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 비회원 주문의 전체 환불 가능 여부를 조회한다. */
@Service
@RequiredArgsConstructor
public class GuestTicketRefundEligibilityService {

    private static final Duration REFUND_CUTOFF = Duration.ofDays(3);

    private final TicketOrderAccessVerifier ticketOrderAccessVerifier;
    private final TicketOrderRepository ticketOrderRepository;
    private final ExpoRepository expoRepository;
    private final IssuedTicketRepository issuedTicketRepository;

    /**
     * {@code readOnly}를 쓰지 않는다 — {@link TicketOrderAccessVerifier#verifyGuestOrderAccess}
     * 가 내부적으로 {@code guest_order_infos} 를 잠그고 실패 횟수를 기록/초기화하는 쓰기를
     * 포함한다(비관적 락 {@code SELECT ... FOR UPDATE} 는 읽기 전용 트랜잭션에서 실행할 수
     * 없다 — PostgreSQL 이 "cannot execute SELECT FOR NO KEY UPDATE in a read-only
     * transaction" 으로 거부한다). 조회 자체가 인증 시도 기록이라는 설계상 이 메서드는
     * 원천적으로 읽기 전용일 수 없다.
     */
    @Transactional
    public TicketRefundEligibilityResponse check(GuestTicketRefundEligibilityRequest request) {
        GuestTicketOrderSearchSnapshot snapshot =
                ticketOrderAccessVerifier.verifyGuestOrderAccess(
                        request.orderNumber(), request.phoneNumber(), request.password());
        TicketOrder order = ticketOrderRepository.findById(snapshot.ticketOrderId()).orElseThrow();
        if (order.getStatus() != TicketOrderStatus.PAID) {
            return ineligible(order, TicketRefundIneligibilityReason.ORDER_NOT_PAID);
        }
        Long expoId = order.getItems().getFirst().getTicketProduct().getExpoId();
        Expo expo = expoRepository.findById(expoId).orElseThrow();
        if (!expo.getEventStartAt().isAfter(Instant.now().plus(REFUND_CUTOFF))) {
            return ineligible(
                    order, TicketRefundIneligibilityReason.EVENT_STARTS_WITHIN_THREE_DAYS);
        }
        if (issuedTicketRepository.existsCheckedInByTicketOrderId(order.getId())) {
            return ineligible(order, TicketRefundIneligibilityReason.TICKET_ALREADY_CHECKED_IN);
        }
        return new TicketRefundEligibilityResponse(true, null, order.getTotalAmount());
    }

    private TicketRefundEligibilityResponse ineligible(
            TicketOrder order, TicketRefundIneligibilityReason reason) {
        return new TicketRefundEligibilityResponse(false, reason, order.getTotalAmount());
    }
}
