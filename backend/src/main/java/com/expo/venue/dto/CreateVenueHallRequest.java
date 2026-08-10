package com.expo.venue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 장소 안에 홀 등록 요청. */
@Schema(description = "홀 등록 요청")
public record CreateVenueHallRequest(
        @Schema(description = "장소 내 홀 코드", example = "HALL-A")
                @NotBlank(message = "홀 코드는 필수입니다.")
                @Size(max = 30, message = "홀 코드는 30자 이하여야 합니다.")
                String hallCode,
        @Schema(description = "홀명", example = "A홀")
                @NotBlank(message = "홀명은 필수입니다.")
                @Size(max = 100, message = "홀명은 100자 이하여야 합니다.")
                String name,
        @Schema(description = "가로 길이(m)")
                @DecimalMin(value = "0.01", message = "가로 길이는 0보다 커야 합니다.")
                BigDecimal width,
        @Schema(description = "세로 길이(m)")
                @DecimalMin(value = "0.01", message = "세로 길이는 0보다 커야 합니다.")
                BigDecimal depth,
        @Schema(description = "홀 배치도 파일 ID") Long layoutFileId) {}
