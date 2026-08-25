package com.expo.banner.service;

import com.expo.common.scheduling.ScheduledJob;
import com.expo.common.scheduling.ScheduledJobRunner;
import com.expo.common.scheduling.SchedulingEnabled;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 배너 노출 상태를 주기적으로 시각에 맞춘다.
 *
 * <p>이것이 없으면 <b>예약 배너가 영원히 안 뜬다.</b> 승인은 시작일이 미래면 {@code SCHEDULED} 로
 * 만들어 두는데, 시작일이 와도 아무도 {@code ACTIVE} 로 바꿔 주지 않기 때문이다.
 *
 * <p>주기는 5분이다. 배너 시작·종료는 분 단위로 촘촘할 이유가 없고, 늦어도 5분 안에는 반영된다.
 * 급할 때는 {@code POST /api/internal/banners/sync-display-status} 로 즉시 부를 수 있다.
 */
@Component
@SchedulingEnabled
class BannerDisplayStatusScheduler {

    private final BannerDisplayStatusService bannerDisplayStatusService;
    private final ScheduledJobRunner scheduledJobRunner;

    BannerDisplayStatusScheduler(
            BannerDisplayStatusService bannerDisplayStatusService,
            ScheduledJobRunner scheduledJobRunner) {
        this.bannerDisplayStatusService = bannerDisplayStatusService;
        this.scheduledJobRunner = scheduledJobRunner;
    }

    @Scheduled(fixedDelayString = "${app.banner.display-sync.fixed-delay-ms:300000}")
    public void sync() {
        scheduledJobRunner.runExclusively(
                ScheduledJob.BANNER_DISPLAY_SYNC, bannerDisplayStatusService::sync);
    }
}
