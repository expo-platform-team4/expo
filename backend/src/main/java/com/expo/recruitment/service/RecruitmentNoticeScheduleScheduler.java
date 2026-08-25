package com.expo.recruitment.service;

import com.expo.common.scheduling.ScheduledJob;
import com.expo.common.scheduling.ScheduledJobRunner;
import com.expo.common.scheduling.SchedulingEnabled;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 모집공고를 신청 시작·종료 시각에 맞춰 주기적으로 전환한다.
 *
 * <p>주기는 5분이다. 공고 신청 시작이 몇 분 늦게 열리는 것은 감당할 수 있고, 마감은 어차피
 * 신청 시점에 기간을 다시 확인한다.
 */
@Component
@SchedulingEnabled
class RecruitmentNoticeScheduleScheduler {

    private final RecruitmentNoticeService recruitmentNoticeService;
    private final ScheduledJobRunner scheduledJobRunner;

    RecruitmentNoticeScheduleScheduler(
            RecruitmentNoticeService recruitmentNoticeService,
            ScheduledJobRunner scheduledJobRunner) {
        this.recruitmentNoticeService = recruitmentNoticeService;
        this.scheduledJobRunner = scheduledJobRunner;
    }

    @Scheduled(fixedDelayString = "${app.recruitment.schedule-sweep.fixed-delay-ms:300000}")
    public void processSchedule() {
        scheduledJobRunner.runExclusively(
                ScheduledJob.RECRUITMENT_NOTICE_SCHEDULE,
                recruitmentNoticeService::processSchedule);
    }
}
