package com.expo.recruitment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** 기업 모집 공고 내용·조건 수정 요청. */
@Schema(description = "기업 모집 공고 수정 요청")
public record UpdateRecruitmentNoticeRequest(
        @Schema(description = "공고 제목")
                @NotBlank(message = "제목은 필수입니다.")
                @Size(max = 255, message = "제목은 255자 이하여야 합니다.")
                String title,
        @Schema(description = "공고 내용") @NotBlank(message = "내용은 필수입니다.") String content,
        @Schema(description = "참가 자격 요건") String eligibility,
        @Schema(description = "제출 자료 요구사항(자유 텍스트, 신청 기업에게 노출됨)") String submissionRequirements,
        @Schema(description = "신청 시작 일시") @NotNull(message = "신청 시작 일시는 필수입니다.")
                Instant applicationStartAt,
        @Schema(description = "신청 종료 일시") @NotNull(message = "신청 종료 일시는 필수입니다.")
                Instant applicationEndAt) {}
