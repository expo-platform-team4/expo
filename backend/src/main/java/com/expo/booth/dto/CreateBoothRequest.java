package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 구역 안에 부스 공간 등록 요청. */
@Schema(description = "부스 공간 등록 요청")
public record CreateBoothRequest(
        @Schema(description = "부스 템플릿 ID") Long boothTemplateId,
        @Schema(description = "구역 내 부스 번호", example = "A-01")
                @NotBlank(message = "부스 번호는 필수입니다.")
                @Size(max = 30, message = "부스 번호는 30자 이하여야 합니다.")
                String boothNumber,
        @Schema(description = "형태 코드", example = "STANDARD-3X3")
                @NotBlank(message = "형태 코드는 필수입니다.")
                @Size(max = 30, message = "형태 코드는 30자 이하여야 합니다.")
                String shapeCode,
        @Schema(description = "가로 길이(m)")
                @NotNull(message = "가로 길이는 필수입니다.")
                @DecimalMin(value = "0.01", message = "가로 길이는 0보다 커야 합니다.")
                @Digits(integer = 6, fraction = 2, message = "가로 길이는 정수부 6자리, 소수부 2자리 이하여야 합니다.")
                BigDecimal width,
        @Schema(description = "높이(m)")
                @DecimalMin(value = "0.01", message = "높이는 0보다 커야 합니다.")
                @Digits(integer = 6, fraction = 2, message = "높이는 정수부 6자리, 소수부 2자리 이하여야 합니다.")
                BigDecimal height,
        @Schema(description = "세로 길이(m)")
                @NotNull(message = "세로 길이는 필수입니다.")
                @DecimalMin(value = "0.01", message = "세로 길이는 0보다 커야 합니다.")
                @Digits(integer = 6, fraction = 2, message = "세로 길이는 정수부 6자리, 소수부 2자리 이하여야 합니다.")
                BigDecimal depth,
        @Schema(description = "치수 단위", example = "M")
                @Size(max = 10, message = "치수 단위는 10자 이하여야 합니다.")
                String dimensionUnit,
        @Schema(description = "구역 내 X 좌표") BigDecimal positionX,
        @Schema(description = "구역 내 Y 좌표") BigDecimal positionY,
        @Schema(description = "회전 각도") BigDecimal rotationDegree,
        @Schema(description = "정렬 순서") Integer sortOrder) {}
