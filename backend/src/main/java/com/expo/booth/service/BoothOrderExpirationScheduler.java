package com.expo.booth.service;

import com.expo.common.scheduling.ScheduledJob;
import com.expo.common.scheduling.ScheduledJobRunner;
import com.expo.common.scheduling.SchedulingEnabled;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 결제 기한이 지난 부스 주문을 주기적으로 만료시킨다.
 *
 * <p>주기가 1분으로 가장 짧다. 만료되지 않은 주문이 <b>부스 자리를 계속 잡고 있어서</b>, 늦어질수록
 * 다른 참가기업이 살 수 있는 부스가 실제보다 적어 보인다.
 */
@Component
@SchedulingEnabled
class BoothOrderExpirationScheduler {

    private final BoothOrderService boothOrderService;
    private final ScheduledJobRunner scheduledJobRunner;

    BoothOrderExpirationScheduler(
            BoothOrderService boothOrderService, ScheduledJobRunner scheduledJobRunner) {
        this.boothOrderService = boothOrderService;
        this.scheduledJobRunner = scheduledJobRunner;
    }

    @Scheduled(fixedDelayString = "${app.booth.order-expiry.fixed-delay-ms:60000}")
    public void expireDue() {
        scheduledJobRunner.runExclusively(
                ScheduledJob.BOOTH_ORDER_EXPIRATION, boothOrderService::expireDue);
    }
}
