package com.expo.booth.dto;

import com.expo.booth.entity.OperationalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 부스 공간 응답. */
@Schema(description = "부스 공간")
public record BoothResponse(
        @Schema(description = "부스 ID") Long id,
        @Schema(description = "상위 구역 ID") Long venueZoneId,
        @Schema(description = "부스 템플릿 ID") Long boothTemplateId,
        @Schema(description = "구역 내 부스 번호") String boothNumber,
        @Schema(description = "형태 코드") String shapeCode,
        @Schema(description = "가로 길이(m)") BigDecimal width,
        @Schema(description = "높이(m)") BigDecimal height,
        @Schema(description = "세로 길이(m)") BigDecimal depth,
        @Schema(description = "치수 단위") String dimensionUnit,
        @Schema(description = "구역 내 X 좌표") BigDecimal positionX,
        @Schema(description = "구역 내 Y 좌표") BigDecimal positionY,
        @Schema(description = "회전 각도") BigDecimal rotationDegree,
        @Schema(description = "정렬 순서") Integer sortOrder,
        @Schema(description = "운영 상태") OperationalStatus operationalStatus,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt) {}
