package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 부스 배정 취소 요청. */
@Schema(description = "부스 배정 취소 요청")
public record CancelBoothAllocationRequest(
        @Schema(description = "취소 사유") @NotBlank(message = "취소 사유는 필수입니다.") String reason) {}
