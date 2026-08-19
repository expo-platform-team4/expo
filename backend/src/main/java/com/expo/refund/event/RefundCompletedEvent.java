package com.expo.refund.event;

import java.math.BigDecimal;

/**
 * 티켓 환불이 완료됐다. 알림 도메인이 받아 SMS 를 보낸다.
 *
 * <h2>발행은 결제·환불 도메인 담당이다</h2>
 *
 * <b>이 이벤트를 발행하는 코드는 아직 없다.</b> 알림 쪽 리스너만 준비돼 있다. 환불 처리를 구현할 때 아래 두 가지를 지켜야 한다.
 *
 * <pre>
 * 1. 환불이 성공한 뒤에만 발행한다 — PG 승인까지 끝난 시점
 * 2. 환불 트랜잭션 안에서 발행한다 — 리스너가 AFTER_COMMIT 으로 받는다
 * </pre>
 *
 * <p>2번이 중요하다. 커밋 전에 발행해도 리스너는 <b>커밋된 뒤에</b> 돌기 때문에, 환불이 롤백되면 문자도 나가지 않는다. 반대로 커밋 밖에서
 * 발행하면 그 보장이 사라진다.
 *
 * <pre>
 * refundService.refund(...) {
 *     ...환불 처리...
 *     eventPublisher.publishEvent(new RefundCompletedEvent(orderId, orderNumber, amount, reason));
 * }   // ← 커밋. 이때 리스너가 돈다
 * </pre>
 *
 * <h2>수신번호를 넘기지 않는다</h2>
 *
 * 알림 도메인이 {@code ticketOrderId} 로 직접 찾는다. 회원·비회원에 따라 출처가 다른 규칙을 발행자마다 다시 구현할 이유가 없다.
 *
 * @param ticketOrderId 환불된 주문. 수신자를 찾는 열쇠다
 * @param orderNumber 주문번호. 문자 본문에 그대로 들어간다
 * @param refundAmount 실제 환불 금액. 예약 수수료 포함 여부는 환불 도메인이 정한 값을 그대로 쓴다
 * @param reason 환불 사유. 없으면 {@code null} — 본문에서 생략된다
 */
public record RefundCompletedEvent(
        Long ticketOrderId, String orderNumber, BigDecimal refundAmount, String reason) {}
