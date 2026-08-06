package com.expo.expo.repository;

import java.time.LocalDate;

/**
 * 목록 카드 조회 Projection (희-SRCH-11: 제목·기간·장소·가격·상태)
 */
public interface ExpoCardProjection {
    Long getExpoId();
    String getTitle();
    LocalDate getStartDate();
    LocalDate getEndDate();
    String getRegion();
    String getVenueName();
    String getThumbnailUrl();
    Integer getMinPrice();     // "15,000원부터" 표기용 MIN(price) — TICKET_TYPE 비고 1
    String getSaleStatus();    // 파생 판매 상태 (희-EXPO-10)
}
