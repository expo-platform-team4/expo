package com.expo.venue.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 홀·구역 배치도 파일 교체 요청. */
@Schema(description = "배치도 교체 요청")
public record UpdateVenueLayoutRequest(
        @Schema(description = "새 배치도 파일 ID (null 이면 배치도를 뗀다)") Long layoutFileId) {}
