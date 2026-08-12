package com.expo.booth.dto;

import com.expo.booth.entity.BoothSalesStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/** 부스 상품 응답. */
@Schema(description = "부스 상품")
public record BoothProductResponse(
        @Schema(description = "부스 상품 ID") Long id,
        @Schema(description = "기업 모집 공고 ID") Long recruitmentNoticeId,
        @Schema(description = "부스 ID") Long boothId,
        @Schema(description = "공급가") BigDecimal supplyPrice,
        @Schema(description = "부가세") BigDecimal vatAmount,
        @Schema(description = "총액") BigDecimal totalPrice,
        @Schema(description = "부가세 포함 여부") boolean vatIncluded,
        @Schema(description = "제공 항목(JSON 문자열)") String includedItems,
        @Schema(description = "판매 시작 일시") Instant salesStartAt,
        @Schema(description = "판매 종료 일시") Instant salesEndAt,
        @Schema(description = "결제 가능 여부") boolean paymentEnabled,
        @Schema(description = "판매 상태") BoothSalesStatus salesStatus,
        @Schema(description = "생성 일시") Instant createdAt,
        @Schema(description = "수정 일시") Instant updatedAt) {}
