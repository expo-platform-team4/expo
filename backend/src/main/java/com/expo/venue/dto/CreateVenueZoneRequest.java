package com.expo.venue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 홀 안에 구역 등록 요청. */
@Schema(description = "구역 등록 요청")
public record CreateVenueZoneRequest(
        @Schema(description = "홀 내 구역 코드", example = "ZONE-1")
                @NotBlank(message = "구역 코드는 필수입니다.")
                @Size(max = 30, message = "구역 코드는 30자 이하여야 합니다.")
                String zoneCode,
        @Schema(description = "구역명", example = "1구역")
                @NotBlank(message = "구역명은 필수입니다.")
                @Size(max = 100, message = "구역명은 100자 이하여야 합니다.")
                String name,
        @Schema(description = "최대 부스 수", example = "20")
                @NotNull(message = "최대 부스 수는 필수입니다.")
                @Min(value = 0, message = "최대 부스 수는 0 이상이어야 합니다.")
                Integer maxBoothCount,
        @Schema(description = "가로 길이(m)")
                @DecimalMin(value = "0.01", message = "가로 길이는 0보다 커야 합니다.")
                BigDecimal width,
        @Schema(description = "세로 길이(m)")
                @DecimalMin(value = "0.01", message = "세로 길이는 0보다 커야 합니다.")
                BigDecimal depth,
        @Schema(description = "구역 배치도 파일 ID") Long layoutFileId) {}
