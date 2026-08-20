package com.expo.expo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/** 홈·목록의 박람회 카드 한 장 ({@code v_public_expo_cards} 뷰 기반). */
@Schema(description = "공개 박람회 카드")
public record PublicExpoCardResponse(
        @Schema(description = "박람회 ID") Long expoId,
        @Schema(description = "박람회명") String title,
        @Schema(description = "행사 시작 시각") Instant eventStartAt,
        @Schema(description = "행사 종료 시각") Instant eventEndAt,
        @Schema(description = "지역 코드") String regionCode,
        @Schema(description = "최저가. 티켓 상품이 없으면 null") BigDecimal minimumPrice,
        @Schema(description = "잔여 수량") long availableQuantity,
        @Schema(description = "노출용 판매 상태", example = "ON_SALE") String displaySalesStatus,
        @Schema(description = "대표 이미지 경로. 없으면 null", example = "/api/files/12/content")
                String thumbnailUrl) {}
