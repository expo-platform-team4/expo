package com.expo.notification.service;

import com.expo.notification.dto.NotificationRequest;
import com.expo.refund.event.RefundCompletedEvent;
import org.springframework.stereotype.Service;

/**
 * 환불 완료 SMS.
 *
 * <p>발권 알림과 다른 점은 <b>수신자를 직접 찾는다</b>는 것뿐이다. 발권은 이미 주문을 읽었지만, 환불 이벤트는 {@code orderId} 만
 * 들고 온다 — 그 이유는 {@link RefundCompletedEvent} 주석에 있다.
 */
@Service
public class RefundCompletedNotificationService {

    private static final String TEMPLATE_CODE = "REFUND_COMPLETED";

    /**
     * 참조 대상을 {@code ORDER} 로 둔다.
     *
     * <p>{@code REFUND} 로 두는 편이 정확해 보이지만, 그러려면 {@code ticket_refunds.id} 를 이벤트에 실어야 한다.
     * 환불은 주문당 하나라({@code ticket_refunds.ticket_order_id} 가 UNIQUE) 주문 ID 로도 1:1 로 찾아갈 수 있고,
     * 발권 알림과 같은 축으로 묶여 "이 주문에 어떤 알림이 나갔나" 를 한 번에 볼 수 있다.
     */
    private static final String REFERENCE_TYPE = "ORDER";

    private final NotificationDispatcher dispatcher;
    private final NotificationRecipientReader recipientReader;
    private final RefundCompletedMessageComposer messageComposer;

    public RefundCompletedNotificationService(
            NotificationDispatcher dispatcher,
            NotificationRecipientReader recipientReader,
            RefundCompletedMessageComposer messageComposer) {
        this.dispatcher = dispatcher;
        this.recipientReader = recipientReader;
        this.messageComposer = messageComposer;
    }

    /** 환불 완료를 받아 SMS 를 보낸다. */
    public void notifyRefundCompleted(RefundCompletedEvent event) {
        dispatcher.dispatch(
                new NotificationRequest(
                        recipientReader.forOrder(event.ticketOrderId()),
                        TEMPLATE_CODE,
                        REFERENCE_TYPE,
                        event.ticketOrderId(),
                        messageComposer.payloadJson(event),
                        messageComposer.smsText(event)));
    }
}
