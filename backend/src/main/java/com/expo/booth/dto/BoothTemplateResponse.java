package com.expo.booth.dto;

import com.expo.booth.entity.OperationalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/** 부스 템플릿 응답. */
@Schema(description = "부스 템플릿")
public record BoothTemplateResponse(
        @Schema(description = "부스 템플릿 ID") Long id,
        @Schema(description = "형태 코드") String shapeCode,
        @Schema(description = "템플릿명") String name,
        @Schema(description = "가로 길이(m)") BigDecimal width,
        @Schema(description = "높이(m)") BigDecimal height,
        @Schema(description = "세로 길이(m)") BigDecimal depth,
        @Schema(description = "치수 단위") String dimensionUnit,
        @Schema(description = "기본 제공 항목(JSON 문자열)") String defaultIncludedItems,
        @Schema(description = "운영 상태") OperationalStatus operationalStatus,
        @Schema(description = "생성 일시") Instant createdAt,
        @Schema(description = "수정 일시") Instant updatedAt) {}
