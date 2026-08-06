package com.expo.expo.dto;

import com.expo.expo.domain.SaleStatus;

import java.time.LocalDate;

/**
 * 박람회 목록 검색 조건 (희-SRCH-01 ~ 10)
 *
 * @param keyword    제목 검색어 (희-SRCH-01)
 * @param categoryId 카테고리 필터 (희-SRCH-02)
 * @param region     지역 필터 (희-SRCH-03)
 * @param fromDate   기간 필터 시작 (희-SRCH-04)
 * @param toDate     기간 필터 종료 (희-SRCH-04)
 * @param minPrice   최저가 하한 (희-SRCH-05)
 * @param maxPrice   최저가 상한 (희-SRCH-05)
 * @param saleStatus 판매 상태 필터 (희-SRCH-06)
 * @param sort       정렬 (희-SRCH-07~09)
 */
public record ExpoSearchCondition(
    String keyword,
    Long categoryId,
    String region,
    LocalDate fromDate,
    LocalDate toDate,
    Integer minPrice,
    Integer maxPrice,
    SaleStatus saleStatus,
    ExpoSort sort
) {

    /** 정렬 기준 */
    public enum ExpoSort {
        /** 최신순 — created_at DESC (희-SRCH-07) */
        LATEST,
        /** 마감 임박순 — end_date ASC (희-SRCH-08) */
        DEADLINE,
        /** 인기순 — 누적 판매수량 DESC (희-SRCH-09) */
        POPULAR
    }

    public ExpoSort sortOrDefault() {
        return sort == null ? ExpoSort.LATEST : sort;
    }
}
