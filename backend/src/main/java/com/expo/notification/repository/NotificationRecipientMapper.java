package com.expo.notification.repository;

import com.expo.notification.dto.ExpoCancelTarget;
import com.expo.notification.dto.NotificationRecipient;
import java.util.List;
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

    /**
     * 박람회 취소 안내를 보낼 대상. <b>수신번호 단위로 중복이 제거된 목록</b>이다.
     *
     * <p>세 가지를 걸러낸다.
     *
     * <ul>
     *   <li>같은 번호는 <b>1행</b> — 티켓이 몇 장이든, 주문이 몇 건이든 문자는 한 통이다
     *   <li>이미 취소·무효화된 티켓은 제외 — 한 주문의 티켓 중 하나라도 살아 있으면 남는다
     *   <li>이미 환불된 주문은 제외 — 환불 완료 문자를 받은 사람에게 "일괄 환불 예정" 을 또 보내면 안 된다
     * </ul>
     *
     * <p>수신번호가 없는 대상도 <b>목록에 남긴다.</b> 걸러 버리면 "보내려 했으나 번호가 없었다" 는 기록이
     * 사라진다. 그 판단은 발송기가 하고 {@code CANCELED} 로 남긴다.
     */
    List<ExpoCancelTarget> findExpoCancelTargets(@Param("expoId") Long expoId);
}
