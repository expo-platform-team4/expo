package com.expo.expo.entity;

/**
 * 개최 신청이 희망하는 장소.
 *
 * <p>{@code venueId} 는 필수다 — 승인 시 만드는 {@code expos.region_code} 가 NOT NULL 인데 신청서에 지역 컬럼이
 * 없어 장소에서 파생시켜야 하기 때문이다. 홀·구역은 "미정" 을 허용한다.
 */
public record DesiredVenue(Long venueId, Long hallId, Long zoneId) {}
