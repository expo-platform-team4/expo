package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 부스 콘텐츠 보완 요청. */
@Schema(description = "부스 콘텐츠 보완 요청")
public record RequestBoothContentCorrectionRequest(
        @Schema(description = "보완 요청 사유") @NotBlank(message = "보완 요청 사유는 필수입니다.") String message) {}
