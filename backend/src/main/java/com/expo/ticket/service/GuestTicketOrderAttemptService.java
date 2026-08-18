package com.expo.ticket.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.ticket.entity.GuestOrder;
import com.expo.ticket.repository.GuestOrderInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestTicketOrderAttemptService {

    private final GuestOrderInfoRepository guestOrderInfoRepository;

    @Transactional
    public void recordFailure(Long ticketOrderId) {
        findAndValidateNotLocked(ticketOrderId).recordFailedAttempt();
    }

    @Transactional
    public void resetFailures(Long ticketOrderId) {
        findAndValidateNotLocked(ticketOrderId).resetFailedAttempts();
    }

    private GuestOrder findAndValidateNotLocked(Long ticketOrderId) {
        GuestOrder guestOrder =
                guestOrderInfoRepository
                        .findByIdForUpdate(ticketOrderId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.GUEST_ORDER_LOOKUP_FAILED));
        guestOrder.validateNotLocked();
        return guestOrder;
    }
}
