package com.expo.notification.service;

import com.expo.notification.dto.NotificationRecipient;
import com.expo.notification.repository.NotificationRecipientMapper;
import org.springframework.stereotype.Component;

/**
 * 주문으로 수신자를 찾는다. 못 찾아도 예외를 던지지 않는다.
 *
 * <h2>왜 발행자에게 안 받나</h2>
 *
 * 환불·박람회 취소 이벤트는 결제·박람회 도메인이 발행한다. 그쪽에 "수신번호도 같이 주세요" 를 요구하면 <b>발행자마다 수신자 판정을 다시
 * 구현하게 된다.</b> 회원이면 {@code users}, 비회원이면 {@code guest_order_infos} 를 보는 규칙이 여기저기 흩어진다.
 *
 * <p>발행자 계약은 {@code orderId} 하나로 두고, 수신자를 찾는 일은 알림 도메인이 맡는다.
 */
@Component
public class NotificationRecipientReader {

    private final NotificationRecipientMapper recipientMapper;

    public NotificationRecipientReader(NotificationRecipientMapper recipientMapper) {
        this.recipientMapper = recipientMapper;
    }

    /**
     * 주문의 수신자를 읽는다.
     *
     * <p>주문이 없으면 <b>예외 대신 "보낼 수 없는 수신자"</b> 를 돌려준다. 알림은 이미 끝난 일에 딸려 오는 후처리라, 여기서 터지면
     * 원인을 찾기 어려운 곳에서 예외가 올라온다. 못 보낸 사실은 {@code CANCELED} 알림으로 남으므로 추적은 된다.
     */
    public NotificationRecipient forOrder(Long ticketOrderId) {
        NotificationRecipient found = recipientMapper.findByTicketOrderId(ticketOrderId);
        return found != null ? found : NotificationRecipient.unreachable(null);
    }
}
