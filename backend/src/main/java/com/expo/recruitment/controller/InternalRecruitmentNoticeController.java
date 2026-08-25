package com.expo.recruitment.controller;

import com.expo.common.response.ApiResponse;
import com.expo.recruitment.dto.RecruitmentNoticeScheduleSweepResult;
import com.expo.recruitment.service.RecruitmentNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내부 배치·운영 호출.
 *
 * <h2>인가를 반드시 명시해야 하는 경로다</h2>
 *
 * {@code SecurityConfig} 의 마지막 규칙이 {@code anyRequest().permitAll()} 이다. {@code
 * /api/internal/**} 에 {@code hasRole("ADMIN")} 을 명시해 두었다.
 *
 * <h2>스케줄러가 이것을 부른다</h2>
 *
 * {@code RecruitmentNoticeScheduleScheduler} 가 주기적으로 같은 작업을 실행한다. {@code ScheduledJobRunner} 가
 * PostgreSQL 어드바이저리 락으로 <b>인스턴스 하나에서만</b> 돌게 막는다.
 *
 * <p>그래도 이 API 는 남겨 둔다. 다음 주기를 기다리지 않고 <b>지금 당장 반영해야 할 때</b>와,
 * 스케줄러가 도는지 의심스러울 때 손으로 확인할 수단이 필요하다.
 */
@Tag(name = "Internal Recruitment Notice", description = "모집공고 일정 자동 전환 (내부 호출)")
@RestController
@RequestMapping("/api/internal/recruitment-notices")
public class InternalRecruitmentNoticeController {

    private final RecruitmentNoticeService recruitmentNoticeService;

    public InternalRecruitmentNoticeController(RecruitmentNoticeService recruitmentNoticeService) {
        this.recruitmentNoticeService = recruitmentNoticeService;
    }

    @Operation(
            summary = "예정된 게시 시작·신청 마감 일괄 처리",
            description =
                    "신청 시작일이 지난 SCHEDULED 공고를 OPEN 으로, 신청 종료일이 지난 OPEN 공고를 CLOSED 로"
                            + " 전환합니다. 여러 번 호출해도 이미 전환된 공고는 다시 처리하지 않습니다.")
    @PostMapping("/process-schedule")
    public ResponseEntity<ApiResponse<RecruitmentNoticeScheduleSweepResult>> processSchedule() {
        return ResponseEntity.ok(ApiResponse.ok(recruitmentNoticeService.processSchedule()));
    }
}
