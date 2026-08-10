package com.expo.venue.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 장소·기간 예약 가능 여부 응답. */
@Schema(description = "장소 가용성")
public record VenueAvailabilityResponse(
        @Schema(description = "해당 기간에 예약 가능한지 여부") boolean available) {}
