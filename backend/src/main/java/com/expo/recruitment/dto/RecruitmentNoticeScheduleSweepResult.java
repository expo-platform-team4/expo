package com.expo.recruitment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 예정된 게시 시작·신청 마감 일괄 처리 결과. */
@Schema(description = "모집공고 일정 자동 전환 결과")
public record RecruitmentNoticeScheduleSweepResult(
        @Schema(description = "SCHEDULED 에서 OPEN 으로 전환한 공고 ID") List<Long> activatedNoticeIds,
        @Schema(description = "신청 종료일이 지나 자동 마감한 공고 ID") List<Long> expiredNoticeIds) {}
