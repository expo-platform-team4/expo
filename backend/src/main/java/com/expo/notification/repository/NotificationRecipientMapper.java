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
     *   <li>구매자가 개별 취소한 티켓({@code CANCELED})은 제외 — {@code INVALIDATED} 는 <b>포함</b>한다.
     *       리스너가 {@code AFTER_COMMIT} 이라 취소 처리가 무효화까지 끝낸 뒤에 이 조회가 돈다.
     *       빼 버리면 정작 취소된 박람회에서 대상이 0명이 된다
     *   <li>환불이 {@code COMPLETED} 인 주문은 제외 — 환불 완료 문자를 받은 사람에게 "일괄 환불 예정" 을
     *       또 보내면 안 된다. {@code REQUESTED}·{@code FAILED} 는 아직 못 받은 사람이라 남긴다
     * </ul>
     *
     * <p>수신번호가 없는 대상도 <b>목록에 남긴다.</b> 걸러 버리면 "보내려 했으나 번호가 없었다" 는 기록이
     * 사라진다. 그 판단은 발송기가 하고 {@code CANCELED} 로 남긴다.
     */
    List<ExpoCancelTarget> findExpoCancelTargets(@Param("expoId") Long expoId);

    /**
     * 박람회 행을 잠근다. <b>취소 안내를 한 번만 보내기 위한 것</b>이다.
     *
     * <p>"이미 보냈나" 를 세어 보고 판단하는 check-then-act 라, 잠그지 않으면 두 요청이 나란히 "아직
     * 안 보냄" 을 보고 양쪽 다 보낸다. 발권·체크인에서 같은 구조를 이미 두 번 겪었다.
     *
     * <p>여기서만 <b>DB 제약으로 대신할 수 없다.</b> 취소 안내는 수신자마다 1행이라
     * {@code (template, type, expoId)} 가 N행 있는 것이 정상이고, 그래서 UNIQUE 를 걸 수 없다.
     * 잠금이 유일한 수단이다.
     *
     * <p><b>호출자의 트랜잭션 안에서 불러야 한다.</b> 잠금은 트랜잭션이 끝나면 풀리므로, 별도 트랜잭션에서
     * 잠그면 돌아오는 순간 이미 풀려 있다.
     *
     * @return 박람회가 있으면 {@code 1}, 없으면 {@code null}
     */
    Integer lockExpo(@Param("expoId") Long expoId);
}
