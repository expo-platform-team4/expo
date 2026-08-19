package com.expo.notification.service;

import com.expo.checkin.dto.TicketIssueResult;
import com.expo.notification.dto.NotificationRecipient;
import com.expo.notification.dto.NotificationRequest;
import org.springframework.stereotype.Service;

/**
 * 발권 완료 SMS.
 *
 * <p>보내고 기록하는 일은 {@link NotificationDispatcher} 가 한다. 여기서는 <b>무엇을 보낼지만</b> 정한다 — 수신자,
 * 템플릿, 문구.
 *
 * <p>수신자를 따로 조회하지 않는다. 발권이 이미 주문을 읽으면서 번호까지 가져왔고, 그 값이 {@link TicketIssueResult} 에 담겨
 * 온다. 환불 알림은 사정이 달라 {@link NotificationRecipientReader} 로 직접 찾는다.
 */
@Service
public class TicketIssuedNotificationService {

    private static final String TEMPLATE_CODE = "TICKET_ISSUED";
    private static final String REFERENCE_TYPE = "ORDER";

    private final NotificationDispatcher dispatcher;
    private final TicketIssuedMessageComposer messageComposer;

    public TicketIssuedNotificationService(
            NotificationDispatcher dispatcher, TicketIssuedMessageComposer messageComposer) {
        this.dispatcher = dispatcher;
        this.messageComposer = messageComposer;
    }

    /** 발권 결과를 받아 SMS 를 보낸다. */
    public void notifyTicketIssued(TicketIssueResult result) {
        dispatcher.dispatch(
                new NotificationRequest(
                        new NotificationRecipient(
                                result.memberUserId(), result.recipientPhoneNumber()),
                        TEMPLATE_CODE,
                        REFERENCE_TYPE,
                        result.orderId(),
                        messageComposer.payloadJson(result),
                        messageComposer.smsText(result)));
    }
}
