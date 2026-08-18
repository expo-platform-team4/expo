package com.expo.venue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * 모집공고 생성 요청 경로의 확정 장소 예약 생성 요청.
 *
 * <p>장소·홀·고른 구역 목록은 이미 승인된 {@code noticeRequestId}에 다 들어있어 다시 받지 않는다. 그 요청이 고른 구역
 * 개수만큼 예약이 한 번에 확정된다.
 */
@Schema(description = "확정 장소 예약 생성 요청")
public record CreateVenueReservationRequest(
        @Schema(description = "근거가 되는 모집공고 생성 요청 ID") @NotNull(message = "모집공고 생성 요청은 필수입니다.")
                Long noticeRequestId,
        @Schema(description = "사용 시작 일시") @NotNull(message = "사용 시작 일시는 필수입니다.") Instant useStartAt,
        @Schema(description = "사용 종료 일시") @NotNull(message = "사용 종료 일시는 필수입니다.")
                Instant useEndAt) {}
