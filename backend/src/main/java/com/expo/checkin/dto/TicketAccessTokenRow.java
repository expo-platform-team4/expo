package com.expo.checkin.dto;

import java.time.Instant;

/**
 * 접근 토큰 조회 결과 투영. 토큰 원문이 아니라 <b>해시로</b> 찾아온다.
 *
 * <p>엔티티가 아니라 투영을 쓰는 이유는, 검증에 필요한 값이 넷뿐이고 여기서 조회한 뒤 곧바로 주문의 티켓을 읽어야 하기 때문이다. 엔티티를 들고 있어도 할 일이
 * 없다.
 *
 * @param id {@code ticket_access_tokens.id}. 접근 기록을 갱신할 때 쓴다
 * @param ticketOrderId 이 토큰이 열어 주는 주문
 * @param orderNumber 주문번호. 응답에 그대로 나간다
 * @param status {@code ACTIVE} / {@code EXPIRED} / {@code REVOKED}
 * @param expiresAt 만료 시각. {@code status} 가 {@code ACTIVE} 여도 이 시각이 지났으면 만료다
 */
public record TicketAccessTokenRow(
        Long id, Long ticketOrderId, String orderNumber, String status, Instant expiresAt) {}
