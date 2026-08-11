package com.expo.participation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 참여 신청서 보완 요청. */
@Schema(description = "보완 요청")
public record RequestCorrectionRequest(
        @Schema(description = "보완 요청 사유") @NotBlank(message = "보완 요청 사유는 필수입니다.") String message) {}
