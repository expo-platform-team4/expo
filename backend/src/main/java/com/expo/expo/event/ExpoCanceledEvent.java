package com.expo.expo.event;

/**
 * 박람회가 취소됐다. 알림 도메인이 받아 관람객 전원에게 안내 SMS 를 보낸다.
 *
 * <h2>발행은 박람회 도메인 담당이다</h2>
 *
 * <b>이 이벤트를 발행하는 코드는 아직 없다.</b> 알림 쪽 리스너만 준비돼 있다. 박람회 취소를 구현할 때
 * 아래 셋을 지켜야 한다.
 *
 * <pre>
 * 1. 취소가 확정된 뒤에만 발행한다
 * 2. 취소 트랜잭션 안에서 발행한다 — 리스너가 AFTER_COMMIT 으로 받는다
 * 3. 티켓을 INVALIDATED 로 바꾼다면 그 <b>전에</b> 발행한다
 * </pre>
 *
 * <p>3번이 이 이벤트에만 있는 조건이다. 대상 추리기가 {@code issued_tickets.status} 가
 * {@code ISSUED}·{@code CHECKED_IN} 인 것만 고르기 때문에, 티켓을 먼저 무효화하면
 * <b>보낼 대상이 0명이 된다.</b> 아무도 안내를 못 받는다.
 *
 * <p>순서를 바꾸기 어렵다면 대상 추리기 조건을 함께 고쳐야 한다
 * ({@code NotificationRecipientMapper.findExpoCancelTargets}).
 *
 * <h2>수신자를 넘기지 않는다</h2>
 *
 * 알림 도메인이 {@code expoId} 로 직접 찾는다. 회원·비회원에 따라 출처가 다른 규칙과, 이미 환불된
 * 주문을 빼는 규칙을 발행자가 알 필요가 없다.
 *
 * @param expoId 취소된 박람회. 대상을 찾는 열쇠이자 알림의 참조 대상이다
 * @param expoTitle 박람회명. 문자 본문에 들어간다
 * @param reason 취소 사유. 없으면 {@code null} — 본문에서 생략된다
 */
public record ExpoCanceledEvent(Long expoId, String expoTitle, String reason) {}
