package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 부스 상품 주문 생성 요청. */
@Schema(description = "부스 상품 주문 생성 요청")
public record CreateBoothOrderRequest(
        @Schema(description = "참여 신청서 ID") @NotNull(message = "신청서 ID는 필수입니다.")
                Long applicationId) {}
