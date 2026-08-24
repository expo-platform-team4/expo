package com.expo.expo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** 박람회 개최 신청 수정 요청. 임시저장 상태에서만 가능하다. */
@Schema(description = "박람회 개최 신청 수정 요청")
public record UpdateExpoOpeningRequestRequest(
        @Valid @NotNull(message = "신청 내용은 필수입니다.") ExpoOpeningRequestPayload content) {}
