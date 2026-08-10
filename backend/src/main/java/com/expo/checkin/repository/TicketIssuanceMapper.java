package com.expo.checkin.repository;

import com.expo.checkin.dto.TicketIssuanceOrder;
import com.expo.checkin.dto.TicketIssuanceOrderItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 발권에 필요한 주문 정보를 읽는다.
 *
 * <p>order 도메인에 엔티티가 아직 없어 JPA 로는 읽을 수 없다. 있더라도 발권은 주문을 <b>읽기만</b> 하므로 MyBatis 투영이 맞다 — order
 * 담당자가 엔티티를 어떻게 설계하든 이 코드는 영향을 받지 않는다.
 */
@Mapper
public interface TicketIssuanceMapper {

    /** 발권 대상 주문. 없으면 {@code null}. */
    TicketIssuanceOrder findOrderForIssuance(Long ticketOrderId);

    /** 주문의 항목 목록. 항목마다 {@code quantity} 만큼 발권한다. */
    List<TicketIssuanceOrderItem> findOrderItems(Long ticketOrderId);

    /**
     * 티켓 코드 일련번호를 시퀀스에서 받는다.
     *
     * <p>애플리케이션에서 COUNT 로 세면 동시 발권에 같은 값이 나오고, {@code ticket_code} 가 UNIQUE 라 그때 INSERT 가 깨진다.
     * 채번은 DB 에 맡긴다.
     */
    long nextTicketCodeSequence();

    /**
     * 이 주문에 이미 발권된 티켓이 있는지 센다.
     *
     * <p>결제 웹훅은 재시도된다. 같은 주문으로 두 번 호출되면 티켓이 두 배로 발급되므로 서비스가 이 값으로 막는다.
     */
    int countIssuedTickets(Long ticketOrderId);
}
