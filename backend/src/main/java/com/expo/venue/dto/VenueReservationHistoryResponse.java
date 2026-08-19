package com.expo.venue.dto;

import com.expo.venue.entity.VenueReservationActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 장소 예약 확정·해제 이력 응답. */
@Schema(description = "장소 예약 이력")
public record VenueReservationHistoryResponse(
        @Schema(description = "이력 ID") Long id,
        @Schema(description = "처리 종류") VenueReservationActionType actionType,
        @Schema(description = "사유") String reason,
        @Schema(description = "처리 관리자 ID") Long processedByAdminId,
        @Schema(description = "생성 일시") Instant createdAt) {}
