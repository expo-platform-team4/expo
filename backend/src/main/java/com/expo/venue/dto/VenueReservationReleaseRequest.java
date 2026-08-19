package com.expo.venue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 확정 장소 예약 해제 요청. */
@Schema(description = "장소 예약 해제 요청")
public record VenueReservationReleaseRequest(
        @Schema(description = "해제 사유") @NotBlank(message = "해제 사유는 필수입니다.") String reason) {}
