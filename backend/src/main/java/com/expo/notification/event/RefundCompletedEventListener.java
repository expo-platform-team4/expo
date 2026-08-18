package com.expo.notification.event;

import com.expo.notification.service.RefundCompletedNotificationService;
import com.expo.refund.event.RefundCompletedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 환불이 커밋된 뒤 SMS 를 보낸다.
 *
 * <p>{@link TicketIssuedEventListener} 와 같은 이유로 {@code AFTER_COMMIT} 이다 — 외부 HTTP 를 환불
 * 트랜잭션 안에서 하면 대행사가 느릴 때 DB 커넥션이 잡히고, 문자 실패로 환불을 되돌릴 이유도 없다.
 *
 * <p><b>아직 이 이벤트를 발행하는 코드가 없다.</b> 결제·환불 도메인이 0줄이다. 발행 시점의 요구사항은
 * {@link RefundCompletedEvent} 주석에 적어 뒀다.
 */
@Component
public class RefundCompletedEventListener {

    private final RefundCompletedNotificationService notificationService;

    public RefundCompletedEventListener(RefundCompletedNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRefundCompleted(RefundCompletedEvent event) {
        notificationService.notifyRefundCompleted(event);
    }
}
