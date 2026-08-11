package com.expo.checkin.event;

import com.expo.checkin.dto.TicketIssueResult;

/**
 * 발권이 끝났을 때 발행된다. {@code notification} 이 받아 SMS 를 보낸다.
 *
 * <p>발권 서비스가 알림 서비스를 직접 부르지 않고 이벤트를 쓰는 이유는 둘이다.
 *
 * <ul>
 *   <li><b>외부 HTTP 를 발권 트랜잭션 밖으로 뺀다.</b> 수신자가 {@code AFTER_COMMIT} 으로 받으므로 Solapi 가 느려도
 *       DB 커넥션이 잡히지 않는다
 *   <li><b>문자 실패가 발권을 되돌리지 않는다.</b> 이미 커밋된 뒤라 롤백 자체가 불가능하다
 * </ul>
 *
 * <p>{@code result} 안에 <b>접근 토큰 원문</b>이 들어 있다. DB 에는 해시만 있어 여기서만 얻을 수 있는 값이므로
 * <b>로그에 남기지 않는다.</b>
 */
public record TicketIssuedEvent(TicketIssueResult result) {}
