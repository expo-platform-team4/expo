package com.expo.settlement.service;

import com.expo.common.scheduling.ScheduledJob;
import com.expo.common.scheduling.ScheduledJobRunner;
import com.expo.common.scheduling.SchedulingEnabled;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정산 대상을 주기적으로 만든다.
 *
 * <h2>이것만 하루 한 번이다</h2>
 *
 * 대상 조건이 <b>"행사가 끝난 지 7일"</b> 이라 분 단위로 볼 이유가 없다. 몇 시간 늦게 만들어져도
 * 아무 차이가 없는 유일한 작업이다.
 *
 * <p>새벽에 도는 이유는 이 작업이 박람회마다 트랜잭션을 따로 열어 <b>다른 작업보다 무겁기</b>
 * 때문이다. 사람이 쓰는 시간대를 피한다. 기준 시간대를 못 박아 둔다 — 서버 타임존이 바뀌면
 * 실행 시각이 조용히 옮겨간다.
 */
@Component
@SchedulingEnabled
class SettlementGenerationScheduler {

    private final SettlementGenerationService settlementGenerationService;
    private final ScheduledJobRunner scheduledJobRunner;

    SettlementGenerationScheduler(
            SettlementGenerationService settlementGenerationService,
            ScheduledJobRunner scheduledJobRunner) {
        this.settlementGenerationService = settlementGenerationService;
        this.scheduledJobRunner = scheduledJobRunner;
    }

    @Scheduled(cron = "${app.settlement.generation.cron:0 30 3 * * *}", zone = "Asia/Seoul")
    public void generateDue() {
        scheduledJobRunner.runExclusively(
                ScheduledJob.SETTLEMENT_GENERATION, settlementGenerationService::generateDue);
    }
}
