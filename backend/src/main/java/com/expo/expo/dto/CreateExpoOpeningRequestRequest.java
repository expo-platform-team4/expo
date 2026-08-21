package com.expo.expo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** 박람회 개최 신청 작성 요청. */
@Schema(description = "박람회 개최 신청 작성 요청")
public record CreateExpoOpeningRequestRequest(
        @Valid @NotNull(message = "신청 내용은 필수입니다.") ExpoOpeningRequestPayload content,
        @Schema(description = "true 면 바로 심사 요청(SUBMITTED), false 면 임시저장(DRAFT)", example = "true")
                boolean submitNow) {}
