package com.expo.expo.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** 상세 조회 한 행. 이미지·자료 목록은 별도 조회라 여기 없다. */
public record PublicExpoDetailRow(
        Long expoId,
        String title,
        String description,
        String regionCode,
        Instant eventStartAt,
        Instant eventEndAt,
        Instant salesStartAt,
        Instant salesEndAt,
        String eventStatus,
        String displaySalesStatus,
        BigDecimal minimumPrice,
        long availableQuantity) {}
