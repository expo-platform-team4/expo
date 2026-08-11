package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 부스 상품 등록 요청. */
@Schema(description = "부스 상품 등록 요청")
public record CreateBoothProductRequest(
        @Schema(description = "기업 모집 공고 ID") @NotNull(message = "모집 공고 ID는 필수입니다.")
                Long recruitmentNoticeId,
        @Schema(description = "부스 ID") @NotNull(message = "부스 ID는 필수입니다.") Long boothId,
        @Schema(description = "공급가")
                @NotNull(message = "공급가는 필수입니다.")
                @DecimalMin(value = "0", message = "공급가는 0 이상이어야 합니다.")
                @Digits(integer = 13, fraction = 2, message = "공급가는 정수부 13자리, 소수부 2자리 이하여야 합니다.")
                BigDecimal supplyPrice,
        @Schema(description = "부가세")
                @DecimalMin(value = "0", message = "부가세는 0 이상이어야 합니다.")
                @Digits(integer = 13, fraction = 2, message = "부가세는 정수부 13자리, 소수부 2자리 이하여야 합니다.")
                BigDecimal vatAmount,
        @Schema(description = "부가세 포함 여부") boolean vatIncluded,
        @Schema(description = "제공 항목(JSON 문자열)") String includedItems,
        @Schema(description = "판매 시작 일시") LocalDateTime salesStartAt,
        @Schema(description = "판매 종료 일시") LocalDateTime salesEndAt,
        @Schema(description = "결제 가능 여부") boolean paymentEnabled) {}
