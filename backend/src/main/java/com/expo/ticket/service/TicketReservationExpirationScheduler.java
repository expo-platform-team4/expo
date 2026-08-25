package com.expo.ticket.service;

import com.expo.common.scheduling.ScheduledJob;
import com.expo.common.scheduling.ScheduledJobRunner;
import com.expo.common.scheduling.SchedulingEnabled;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료 예약 해제 작업을 주기적으로 실행한다.
 *
 * <p>예전에는 아무 보호 없이 돌았다. 인스턴스가 둘이면 <b>같은 예약을 동시에 해제하려 들었다</b> —
 * 저장소에서 유일하게 실제로 도는 스케줄러였는데 정작 중복 실행 대책이 없었다.
 */
@Component
@SchedulingEnabled
@RequiredArgsConstructor
class TicketReservationExpirationScheduler {

    private final TicketReservationExpirationService ticketReservationExpirationService;
    private final ScheduledJobRunner scheduledJobRunner;

    @Scheduled(fixedDelayString = "${app.ticket.reservation-expiry.fixed-delay-ms:60000}")
    public void expireReservations() {
        scheduledJobRunner.runExclusively(
                ScheduledJob.TICKET_RESERVATION_EXPIRATION,
                ticketReservationExpirationService::expireReservations);
    }
}
