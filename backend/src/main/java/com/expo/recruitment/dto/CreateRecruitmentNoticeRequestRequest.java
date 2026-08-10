package com.expo.recruitment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** 모집공고 생성 요청 작성. */
@Schema(description = "모집공고 생성 요청 작성")
public record CreateRecruitmentNoticeRequestRequest(
        @Schema(description = "제목", example = "2026 서울 테크 박람회 참가기업 모집")
                @NotBlank(message = "제목은 필수입니다.")
                @Size(max = 255, message = "제목은 255자 이하여야 합니다.")
                String title,
        @Schema(description = "설명") @NotBlank(message = "설명은 필수입니다.") String description,
        @Schema(description = "신청 시작 일시") @NotNull(message = "신청 시작 일시는 필수입니다.")
                LocalDateTime applicationStartAt,
        @Schema(description = "신청 종료 일시") @NotNull(message = "신청 종료 일시는 필수입니다.")
                LocalDateTime applicationEndAt,
        @Schema(description = "행사 시작 일시") @NotNull(message = "행사 시작 일시는 필수입니다.")
                LocalDateTime eventStartAt,
        @Schema(description = "행사 종료 일시") @NotNull(message = "행사 종료 일시는 필수입니다.")
                LocalDateTime eventEndAt,
        @Schema(description = "희망 가상 장소 ID") @NotNull(message = "가상 장소는 필수입니다.")
                Long virtualVenueId,
        @Schema(description = "희망 홀 ID") Long venueHallId,
        @Schema(description = "희망 구역 ID") Long venueZoneId,
        @Schema(description = "목표 참가 기업 수") @Min(value = 1, message = "목표 참가 기업 수는 1 이상이어야 합니다.")
                Integer targetCompanyCount,
        @Schema(description = "희망 부스 구성(JSON 문자열)") String requestedBoothConfig) {}
