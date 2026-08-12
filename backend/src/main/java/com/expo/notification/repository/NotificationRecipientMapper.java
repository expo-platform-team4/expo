package com.expo.notification.repository;

import com.expo.notification.dto.NotificationRecipient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 주문으로 알림 수신자를 찾는다.
 *
 * <p>발권({@code TicketIssuanceMapper.findOrderForIssuance})도 같은 값을 읽지만 <b>그건 쓸 수 없다.</b>
 * 거기에는 {@code FOR UPDATE} 가 붙어 있어 주문 행을 잠근다 — 알림을 보내려고 주문을 잠그면 그 사이 다른 작업이 막힌다.
 */
@Mapper
public interface NotificationRecipientMapper {

    /**
     * 주문의 수신자. 주문이 없으면 {@code null}.
     *
     * <p>회원·비회원 어느 쪽이든 번호가 없을 수 있다. 그 판단은 {@link NotificationRecipient#reachable()} 이 한다.
     */
    NotificationRecipient findByTicketOrderId(@Param("ticketOrderId") Long ticketOrderId);
}
