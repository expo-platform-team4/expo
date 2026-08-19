package com.expo.recruitment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** 모집공고 생성 요청 작성. */
@Schema(description = "모집공고 생성 요청 작성")
public record CreateRecruitmentNoticeRequestRequest(
        @Schema(description = "제목", example = "2026 서울 테크 박람회 참가기업 모집")
                @NotBlank(message = "제목은 필수입니다.")
                @Size(max = 255, message = "제목은 255자 이하여야 합니다.")
                String title,
        @Schema(description = "설명") @NotBlank(message = "설명은 필수입니다.") String description,
        @Schema(description = "신청 시작 일시") @NotNull(message = "신청 시작 일시는 필수입니다.")
                Instant applicationStartAt,
        @Schema(description = "신청 종료 일시") @NotNull(message = "신청 종료 일시는 필수입니다.")
                Instant applicationEndAt,
        @Schema(description = "행사 시작 일시") @NotNull(message = "행사 시작 일시는 필수입니다.")
                Instant eventStartAt,
        @Schema(description = "행사 종료 일시") @NotNull(message = "행사 종료 일시는 필수입니다.") Instant eventEndAt,
        @Schema(description = "희망 가상 장소 ID") @NotNull(message = "가상 장소는 필수입니다.")
                Long virtualVenueId,
        @Schema(description = "희망 전시관(홀) ID") @NotNull(message = "전시관은 필수입니다.") Long venueHallId,
        @Schema(description = "희망 구역(홀) ID 목록 - 같은 전시관 안에서 하나 이상 선택")
                @NotEmpty(message = "구역은 하나 이상 선택해야 합니다.")
                List<Long> venueZoneIds,
        @Schema(description = "목표 참가 기업 수") @Min(value = 1, message = "목표 참가 기업 수는 1 이상이어야 합니다.")
                Integer targetCompanyCount,
        @Schema(description = "희망 부스 구성(JSON 문자열)") String requestedBoothConfig) {}
