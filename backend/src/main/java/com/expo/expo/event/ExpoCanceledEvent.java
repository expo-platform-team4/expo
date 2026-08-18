package com.expo.expo.event;

/**
 * 박람회가 취소됐다. 알림 도메인이 받아 관람객 전원에게 안내 SMS 를 보낸다.
 *
 * <h2>발행은 박람회 도메인 담당이다</h2>
 *
 * <b>이 이벤트를 발행하는 코드는 아직 없다.</b> 알림 쪽 리스너만 준비돼 있다. 박람회 취소를 구현할 때
 * 아래 둘을 지켜야 한다.
 *
 * <pre>
 * 1. 취소가 확정된 뒤에만 발행한다
 * 2. 취소 트랜잭션 안에서 발행한다 — 리스너가 AFTER_COMMIT 으로 받는다
 * </pre>
 *
 * <p><b>티켓 무효화 순서는 신경 쓰지 않아도 된다.</b> 취소 처리가 티켓을 {@code INVALIDATED} 로
 * 바꾸든 말든, 언제 바꾸든 대상은 그대로 나온다.
 *
 * <h2>한때 "무효화 전에 발행하라" 고 적혀 있었다</h2>
 *
 * 그 지시는 지킬 수 없는 것이었다. {@code AFTER_COMMIT} 리스너는 <b>커밋이 끝난 뒤</b>에 발사되므로,
 * 트랜잭션 안에서 발행을 앞에 두든 뒤에 두든 리스너가 보는 것은 언제나 커밋된 최종 상태다.
 * 발행자가 지시를 성실히 지켜도 대상 조회는 무효화된 티켓을 만나고, <b>대상 0명 · 에러 없음</b>으로
 * 조용히 끝났을 것이다.
 *
 * <p>그래서 대상 조회 쪽을 고쳤다 — {@code CANCELED}(구매자가 개별 취소한 티켓)만 빼고
 * {@code INVALIDATED} 는 대상으로 남긴다. 발행자에게 지킬 수 없는 계약을 요구하는 대신,
 * 알림 도메인이 스스로 견디게 만드는 쪽이 맞다
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
