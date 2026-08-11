package com.expo.checkin.dto;

import java.time.Instant;

/**
 * 발권에 필요한 주문 정보. {@code ticket_orders} 조회 결과 투영이다.
 *
 * <p>order 도메인에 엔티티가 아직 없다. 있더라도 발권은 주문의 일부 필드만 읽으므로 엔티티를 통째로 끌어오는 대신 필요한 값만 투영한다. 이 방향이면 order
 * 담당자의 진행에 컴파일 의존이 생기지 않는다.
 *
 * @param orderId {@code ticket_orders.id}
 * @param orderNumber 주문번호. 로그·SMS 본문에 쓴다
 * @param status 주문 상태. {@code PAID} 가 아니면 발권하지 않는다
 * @param recipientPhoneNumber 수신 번호. 회원은 {@code users.phone_number}, 비회원은 {@code
 *     guest_order_infos.phone_number}. <b>둘 다 없을 수 있다</b> — 소셜 로그인 회원은 번호가 없다
 * @param expoEndAt 박람회 종료 시각. 접근 토큰 만료 기준이다
 */
public record TicketIssuanceOrder(
        Long orderId,
        String orderNumber,
        String status,
        String recipientPhoneNumber,
        Instant expoEndAt) {}
