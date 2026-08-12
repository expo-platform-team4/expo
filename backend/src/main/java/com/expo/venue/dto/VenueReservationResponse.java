package com.expo.venue.dto;

import com.expo.venue.entity.ReservationSourceType;
import com.expo.venue.entity.VenueReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 장소 예약 응답. */
@Schema(description = "장소 예약")
public record VenueReservationResponse(
        @Schema(description = "예약 ID") Long id,
        @Schema(description = "예약 원천 유형") ReservationSourceType reservationSourceType,
        @Schema(description = "근거 모집공고 생성 요청 ID") Long noticeRequestId,
        @Schema(description = "가상 장소 ID") Long virtualVenueId,
        @Schema(description = "홀 ID") Long venueHallId,
        @Schema(description = "구역 ID") Long venueZoneId,
        @Schema(description = "사용 시작 일시") Instant useStartAt,
        @Schema(description = "사용 종료 일시") Instant useEndAt,
        @Schema(description = "예약 상태") VenueReservationStatus status,
        @Schema(description = "확정 관리자 ID") Long confirmedByAdminId,
        @Schema(description = "확정 일시") Instant confirmedAt,
        @Schema(description = "해제 일시") Instant releasedAt) {}
