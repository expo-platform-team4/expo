package com.expo.expo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 박람회 상세 (이슈 #107).
 *
 * <p>카드에 없는 {@code description} 은 {@code expos} 에서 직접 읽는다 — 뷰에는 목록용 필드만 있다.
 */
@Schema(description = "공개 박람회 상세")
public record PublicExpoDetailResponse(
        @Schema(description = "박람회 ID") Long expoId,
        @Schema(description = "박람회명") String title,
        @Schema(description = "상세 소개") String description,
        @Schema(description = "지역 코드") String regionCode,
        @Schema(description = "행사 시작 시각") Instant eventStartAt,
        @Schema(description = "행사 종료 시각") Instant eventEndAt,
        @Schema(description = "판매 시작 시각") Instant salesStartAt,
        @Schema(description = "판매 종료 시각") Instant salesEndAt,
        @Schema(description = "행사 진행 상태") String eventStatus,
        @Schema(description = "노출용 판매 상태") String displaySalesStatus,
        @Schema(description = "최저가") BigDecimal minimumPrice,
        @Schema(description = "잔여 수량") long availableQuantity,
        @Schema(description = "이미지 목록") List<ExpoImageResponse> images,
        @Schema(description = "자료 목록") List<ExpoFileResponse> files) {}
