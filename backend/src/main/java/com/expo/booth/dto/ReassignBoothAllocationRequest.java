package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 부스 확정 배정 재배정 요청. */
@Schema(description = "부스 확정 배정 재배정 요청")
public record ReassignBoothAllocationRequest(
        @Schema(description = "옮겨갈 부스 상품 ID") @NotNull(message = "부스 상품 ID는 필수입니다.")
                Long boothProductId,
        @Schema(description = "재배정 사유") @NotBlank(message = "재배정 사유는 필수입니다.") String reason) {}
