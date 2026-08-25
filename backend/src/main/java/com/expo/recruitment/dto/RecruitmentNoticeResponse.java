package com.expo.recruitment.dto;

import com.expo.recruitment.entity.RecruitmentNoticeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** 기업 모집 공고 응답. */
@Schema(description = "기업 모집 공고")
public record RecruitmentNoticeResponse(
        @Schema(description = "공고 ID") Long id,
        @Schema(description = "근거 모집공고 생성 요청 ID") Long requestId,
        @Schema(description = "주최 클라이언트 ID") Long hostClientId,
        @Schema(description = "이 공고가 어느 박람회를 위한 것인지. 아직 연결 안 됐으면 null") Long expoId,
        @Schema(description = "확정된 전시관(홀) ID") Long venueHallId,
        @Schema(description = "홀 배치도 파일 ID") Long venueHallLayoutFileId,
        @Schema(description = "확정된 구역(홀) ID 목록") List<Long> venueZoneIds,
        @Schema(description = "구역별 배치도 파일 ID 목록 (venueZoneIds 와 같은 순서)")
                List<Long> venueZoneLayoutFileIds,
        @Schema(description = "공고 제목") String title,
        @Schema(description = "공고 내용") String content,
        @Schema(description = "참가 자격 요건") String eligibility,
        @Schema(description = "제출 자료 요구사항(JSON 문자열)") String submissionRequirements,
        @Schema(description = "신청 시작 일시") Instant applicationStartAt,
        @Schema(description = "신청 종료 일시") Instant applicationEndAt,
        @Schema(description = "공고 상태") RecruitmentNoticeStatus status,
        @Schema(description = "게시 일시") Instant publishedAt,
        @Schema(description = "마감 일시") Instant closedAt,
        @Schema(description = "작성 관리자 ID") Long createdByAdminId,
        @Schema(description = "생성 일시") Instant createdAt,
        @Schema(description = "수정 일시") Instant updatedAt) {}
