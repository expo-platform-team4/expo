package com.expo.expo.entity;

import java.time.Instant;

/**
 * 시작·종료 한 쌍. 행사 기간과 티켓 판매 기간에 함께 쓴다.
 *
 * <p>둘을 따로 넘기면 팩토리 파라미터가 금방 열 개를 넘어(Checkstyle 상한 8) 읽기도 어려워진다. DB 에도
 * {@code ck_expo_opening_requests_event}·{@code _sales} 로 "종료가 시작보다 뒤" 라는 제약이 쌍 단위로 걸려 있어,
 * 묶어 다루는 편이 실제 모델과도 맞는다.
 */
public record ExpoPeriod(Instant startAt, Instant endAt) {}
