package com.expo.notification.event;

import com.expo.checkin.event.TicketIssuedEvent;
import com.expo.notification.service.TicketIssuedNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 발권이 커밋된 뒤 SMS 를 보낸다.
 *
 * <p><b>{@code AFTER_COMMIT} 이 핵심이다.</b> 발권 트랜잭션 안에서 외부 HTTP 를 호출하면 대행사가 느릴 때 DB 커넥션이 그동안
 * 잡혀 있다. 커밋 후로 미루면 티켓은 이미 안전하게 저장돼 있고, 문자만 따로 나간다.
 *
 * <p>여기서 예외가 나도 발권은 롤백되지 않는다 — 이미 커밋됐기 때문에 물리적으로 불가능하다. 그게 의도한 동작이다.
 * 문자가 실패했다고 산 티켓을 없앨 이유가 없다.
 */
@Slf4j
@Component
public class TicketIssuedEventListener {

    private final TicketIssuedNotificationService notificationService;

    public TicketIssuedEventListener(TicketIssuedNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketIssued(TicketIssuedEvent event) {
        notificationService.notifyTicketIssued(event.result());
    }
}
