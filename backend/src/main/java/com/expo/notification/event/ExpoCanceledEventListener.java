package com.expo.notification.event;

import com.expo.expo.event.ExpoCanceledEvent;
import com.expo.notification.service.ExpoCanceledNotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 박람회 취소가 커밋된 뒤 관람객 전원에게 안내를 보낸다.
 *
 * <p>{@link TicketIssuedEventListener}·{@link RefundCompletedEventListener} 와 같은 이유로
 * {@code AFTER_COMMIT} 이다 — 외부 HTTP 를 취소 트랜잭션 안에서 하면 대행사가 느릴 때 DB 커넥션이
 * 잡히고, 문자 실패로 취소를 되돌릴 이유도 없다.
 *
 * <p>다만 이쪽은 <b>대상이 N명</b>이라 무게가 다르다. 1,000명 기준으로 잡았고, 그 근거와 한계는
 * {@code expo-docs/07-SCALE.md} 에 있다.
 *
 * <p>커밋 뒤에 도는 만큼 <b>대상 조회가 최종 상태를 본다.</b> 취소 처리가 티켓을 무효화했다면
 * 리스너에게는 이미 무효화된 티켓만 보인다. 그래서 대상 조회는 {@code INVALIDATED} 를 대상에
 * 포함한다 — 발행 순서로는 해결되지 않는 문제라 조회 쪽에서 견딘다.
 *
 * <p><b>아직 이 이벤트를 발행하는 코드가 없다.</b> 박람회 도메인이 0줄이다. 발행 시점의 요구사항은
 * {@link ExpoCanceledEvent} 주석에 적어 뒀다.
 */
@Component
public class ExpoCanceledEventListener {

    private final ExpoCanceledNotificationService notificationService;

    public ExpoCanceledEventListener(ExpoCanceledNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExpoCanceled(ExpoCanceledEvent event) {
        notificationService.notifyExpoCanceled(event);
    }
}
