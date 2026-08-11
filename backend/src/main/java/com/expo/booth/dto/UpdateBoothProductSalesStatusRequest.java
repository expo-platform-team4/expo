package com.expo.booth.dto;

import com.expo.booth.entity.BoothSalesStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 부스 상품 판매 상태 변경 요청. */
@Schema(description = "부스 상품 판매 상태 변경 요청")
public record UpdateBoothProductSalesStatusRequest(
        @Schema(description = "변경할 판매 상태. AVAILABLE·UNAVAILABLE·CANCELED 만 허용")
                @NotNull(message = "판매 상태는 필수입니다.")
                BoothSalesStatus salesStatus) {}
