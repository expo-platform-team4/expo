package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 부스 상품 일괄 등록 요청. */
@Schema(description = "부스 상품 일괄 등록 요청")
public record BulkCreateBoothProductRequest(
        @Schema(description = "등록할 부스 상품 목록") @NotEmpty(message = "등록할 부스 상품이 1개 이상이어야 합니다.")
                List<@NotNull @Valid CreateBoothProductRequest> boothProducts) {}
