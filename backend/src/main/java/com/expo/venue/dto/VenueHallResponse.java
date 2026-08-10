package com.expo.venue.dto;

import com.expo.venue.entity.OperationalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 홀 응답. */
@Schema(description = "홀")
public record VenueHallResponse(
        @Schema(description = "홀 ID") Long id,
        @Schema(description = "상위 장소 ID") Long venueId,
        @Schema(description = "장소 내 홀 코드") String hallCode,
        @Schema(description = "홀명") String name,
        @Schema(description = "가로 길이(m)") BigDecimal width,
        @Schema(description = "세로 길이(m)") BigDecimal depth,
        @Schema(description = "홀 배치도 파일 ID") Long layoutFileId,
        @Schema(description = "운영 상태") OperationalStatus operationalStatus,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt) {}
