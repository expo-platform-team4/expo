package com.expo.ticket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 만료 예약 해제 작업을 주기적으로 실행한다. */
@Component
@RequiredArgsConstructor
class TicketReservationExpirationScheduler {

    private final TicketReservationExpirationService ticketReservationExpirationService;

    @Scheduled(fixedDelayString = "${app.ticket.reservation-expiry.fixed-delay-ms:60000}")
    public void expireReservations() {
        ticketReservationExpirationService.expireReservations();
    }
}
