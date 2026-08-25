package com.expo.expo.dto;

/**
 * 목록 조회 조건.
 *
 * @param regionCode 지역 필터. {@code null} 이면 전체
 * @param keyword 박람회명 부분 일치. {@code null} 이거나 빈 문자열이면 전체
 * @param sort {@code POPULAR} 면 인기순, 그 밖의 값이면 행사일 빠른 순
 * @param categoryId 카테고리 필터. {@code null} 이면 전체 — {@code expo_categories} 에 이 카테고리가
 *     걸린 박람회만 남긴다.
 */
public record PublicExpoQuery(String regionCode, String keyword, String sort, Long categoryId) {

    public static final String SORT_POPULAR = "POPULAR";
}
