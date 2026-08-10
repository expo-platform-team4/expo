package com.expo.venue.dto;

import com.expo.venue.entity.OperationalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 가상 장소 응답. */
@Schema(description = "가상 장소")
public record VirtualVenueResponse(
        @Schema(description = "가상 장소 ID") Long id,
        @Schema(description = "장소명") String name,
        @Schema(description = "주소") String address,
        @Schema(description = "지역 코드") String regionCode,
        @Schema(description = "장소 설명") String description,
        @Schema(description = "전체 배치도 파일 ID") Long mapFileId,
        @Schema(description = "운영 상태") OperationalStatus operationalStatus,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt) {}
