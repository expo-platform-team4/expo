package com.expo.venue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** 모집공고 생성 요청 경로의 확정 장소 예약 생성 요청. */
@Schema(description = "확정 장소 예약 생성 요청")
public record CreateVenueReservationRequest(
        @Schema(description = "근거가 되는 모집공고 생성 요청 ID") @NotNull(message = "모집공고 생성 요청은 필수입니다.")
                Long noticeRequestId,
        @Schema(description = "가상 장소 ID") @NotNull(message = "가상 장소는 필수입니다.") Long virtualVenueId,
        @Schema(description = "홀 ID") Long venueHallId,
        @Schema(description = "구역 ID") Long venueZoneId,
        @Schema(description = "사용 시작 일시") @NotNull(message = "사용 시작 일시는 필수입니다.") Instant useStartAt,
        @Schema(description = "사용 종료 일시") @NotNull(message = "사용 종료 일시는 필수입니다.")
                Instant useEndAt) {}
