package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 부스 템플릿 등록 요청. */
@Schema(description = "부스 템플릿 등록 요청")
public record CreateBoothTemplateRequest(
        @Schema(description = "형태 코드", example = "STANDARD-3X3")
                @NotBlank(message = "형태 코드는 필수입니다.")
                @Size(max = 30, message = "형태 코드는 30자 이하여야 합니다.")
                String shapeCode,
        @Schema(description = "템플릿명", example = "기본 3x3 부스")
                @NotBlank(message = "템플릿명은 필수입니다.")
                @Size(max = 100, message = "템플릿명은 100자 이하여야 합니다.")
                String name,
        @Schema(description = "가로 길이(m)")
                @NotNull(message = "가로 길이는 필수입니다.")
                @DecimalMin(value = "0.01", message = "가로 길이는 0보다 커야 합니다.")
                BigDecimal width,
        @Schema(description = "높이(m)") @DecimalMin(value = "0.01", message = "높이는 0보다 커야 합니다.")
                BigDecimal height,
        @Schema(description = "세로 길이(m)")
                @NotNull(message = "세로 길이는 필수입니다.")
                @DecimalMin(value = "0.01", message = "세로 길이는 0보다 커야 합니다.")
                BigDecimal depth,
        @Schema(description = "치수 단위", example = "M") String dimensionUnit,
        @Schema(description = "기본 제공 항목(JSON 문자열)") String defaultIncludedItems) {}
