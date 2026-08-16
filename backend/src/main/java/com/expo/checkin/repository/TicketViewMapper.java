package com.expo.checkin.repository;

import com.expo.checkin.dto.TicketAccessTokenRow;
import com.expo.checkin.dto.TicketViewTicket;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SMS 링크로 들어온 티켓 조회를 읽는다.
 *
 * <p>발권({@link TicketIssuanceMapper})과 매퍼를 나눈 이유는 방향이 반대여서다. 발권은 주문을 읽어 티켓을 쓰고, 조회는 티켓을 읽기만 한다.
 * 한 매퍼에 두면 "발권용" 이라는 이름이 거짓말이 된다.
 */
@Mapper
public interface TicketViewMapper {

    /**
     * 토큰 <b>해시</b>로 접근 토큰을 찾는다. 없으면 {@code null}.
     *
     * <p>원문으로 찾지 않는다 — DB 에 원문이 없다. 서비스가 원문을 해시해서 넘긴다.
     */
    TicketAccessTokenRow findAccessTokenByHash(@Param("tokenHash") String tokenHash);

    /**
     * 주문에 발급된 입장권 전부. 발권된 적이 없으면 빈 목록.
     *
     * <p>{@code issued_tickets} 는 주문에 직접 붙지 않고 {@code ticket_order_items} 를 거친다.
     */
    List<TicketViewTicket> findTicketsByOrderId(@Param("ticketOrderId") Long ticketOrderId);

    /**
     * 접근 기록을 남긴다. 조회 1회에 1 증가.
     *
     * <p>링크가 언제 몇 번 열렸는지는 "문자를 못 받았다" 는 문의를 가릴 때 유일한 근거다.
     */
    void touchAccessToken(@Param("id") Long id, @Param("accessedAt") Instant accessedAt);
}
