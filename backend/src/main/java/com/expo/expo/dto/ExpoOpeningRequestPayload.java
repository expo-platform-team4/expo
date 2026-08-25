package com.expo.expo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * 박람회 개최 신청 작성·수정 공통 본문.
 *
 * <p>작성과 수정이 받는 값이 완전히 같아 하나로 쓴다. 다른 것은 {@code submitNow}(작성 시에만 의미가 있다) 뿐이라 그것만
 * 작성 요청에서 따로 받는다.
 *
 * <p><b>카테고리는 받는다.</b> {@code expo_opening_request_categories}(승인 전 임시 연결)에 담아 뒀다가
 * 승인 시 {@code expo_categories} 로 그대로 복사한다. <b>대표 이미지·소개 자료는 여전히 받지 않는다</b> — 파일
 * 도메인 자체가 아직 없다(이슈 #93). 이슈 #116 에 남겨 뒀다.
 */
@Schema(description = "박람회 개최 신청 본문")
public record ExpoOpeningRequestPayload(
        @Schema(description = "박람회명", example = "2026 스마트 제조 박람회")
                @NotBlank(message = "박람회명은 필수입니다.")
                @Size(max = 255, message = "박람회명은 255자 이하여야 합니다.")
                String title,
        @Schema(description = "상세 소개") @NotBlank(message = "상세 소개는 필수입니다.") String description,
        @Schema(description = "행사 시작 일시") @NotNull(message = "행사 시작 일시는 필수입니다.")
                Instant eventStartAt,
        @Schema(description = "행사 종료 일시") @NotNull(message = "행사 종료 일시는 필수입니다.") Instant eventEndAt,
        @Schema(description = "티켓 판매 시작 일시") @NotNull(message = "판매 시작 일시는 필수입니다.")
                Instant salesStartAt,
        @Schema(description = "티켓 판매 종료 일시") @NotNull(message = "판매 종료 일시는 필수입니다.")
                Instant salesEndAt,
        @Schema(description = "희망 가상 장소 ID. 승인 시 지역 코드를 여기서 가져오므로 필수다.")
                @NotNull(message = "희망 장소는 필수입니다.")
                Long desiredVenueId,
        @Schema(description = "희망 전시관(홀) ID") Long desiredVenueHallId,
        @Schema(description = "희망 구역 ID") Long desiredVenueZoneId,
        @Schema(description = "카테고리 ID 목록. 선택 항목 — 안 보내면 카테고리 없음으로 처리한다.")
                List<Long> categoryIds) {}
