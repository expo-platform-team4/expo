package com.expo.recruitment.dto;

import com.expo.recruitment.entity.RecruitmentNoticeRequestStatus;
import com.expo.recruitment.entity.VenueConflictStatus;
import com.expo.recruitment.entity.VenueDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 모집공고 생성 요청 응답. */
@Schema(description = "모집공고 생성 요청")
public record RecruitmentNoticeRequestResponse(
        @Schema(description = "요청 ID") Long id,
        @Schema(description = "주최 클라이언트 ID") Long hostClientId,
        @Schema(description = "제목") String title,
        @Schema(description = "설명") String description,
        @Schema(description = "신청 시작 일시") Instant applicationStartAt,
        @Schema(description = "신청 종료 일시") Instant applicationEndAt,
        @Schema(description = "행사 시작 일시") Instant eventStartAt,
        @Schema(description = "행사 종료 일시") Instant eventEndAt,
        @Schema(description = "희망 가상 장소 ID") Long virtualVenueId,
        @Schema(description = "희망 홀 ID") Long venueHallId,
        @Schema(description = "희망 구역 ID") Long venueZoneId,
        @Schema(description = "목표 참가 기업 수") Integer targetCompanyCount,
        @Schema(description = "희망 부스 구성(JSON 문자열)") String requestedBoothConfig,
        @Schema(description = "처리 상태") RecruitmentNoticeRequestStatus status,
        @Schema(description = "장소 충돌 검토 상태") VenueConflictStatus venueConflictStatus,
        @Schema(description = "장소 최종 결정") VenueDecision venueDecision,
        @Schema(description = "생성 일시") Instant createdAt,
        @Schema(description = "수정 일시") Instant updatedAt) {}
