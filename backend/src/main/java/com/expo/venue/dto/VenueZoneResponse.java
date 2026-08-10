package com.expo.venue.dto;

import com.expo.venue.entity.OperationalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 구역 응답. */
@Schema(description = "구역")
public record VenueZoneResponse(
        @Schema(description = "구역 ID") Long id,
        @Schema(description = "상위 홀 ID") Long hallId,
        @Schema(description = "홀 내 구역 코드") String zoneCode,
        @Schema(description = "구역명") String name,
        @Schema(description = "최대 부스 수") Integer maxBoothCount,
        @Schema(description = "가로 길이(m)") BigDecimal width,
        @Schema(description = "세로 길이(m)") BigDecimal depth,
        @Schema(description = "구역 배치도 파일 ID") Long layoutFileId,
        @Schema(description = "운영 상태") OperationalStatus operationalStatus,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt) {}
