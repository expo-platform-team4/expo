package com.expo.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.notification.dto.MessageSendResult;
import com.expo.notification.dto.NotificationRecipient;
import com.expo.notification.entity.Notification;
import com.expo.notification.entity.NotificationStatus;
import com.expo.notification.repository.MessageHistoryRepository;
import com.expo.notification.repository.NotificationRecipientMapper;
import com.expo.notification.repository.NotificationRepository;
import com.expo.refund.event.RefundCompletedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * 환불 알림 규칙을 못박는다.
 *
 * <p>발권 알림과 다른 지점 둘에 집중한다 — <b>수신자를 직접 찾는가</b>, 그리고 <b>사유가 자유 입력이라 payload 가 깨지지 않는가</b>.
 */
class RefundCompletedNotificationServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final String ORDER_NUMBER = "ORD-20260812-0001";

    private final NotificationRepository notificationRepository =
            Mockito.mock(NotificationRepository.class);
    private final MessageHistoryRepository messageHistoryRepository =
            Mockito.mock(MessageHistoryRepository.class);
    private final SmsSender smsSender = Mockito.mock(SmsSender.class);
    private final NotificationRecipientMapper recipientMapper =
            Mockito.mock(NotificationRecipientMapper.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RefundCompletedNotificationService service;

    @BeforeEach
    void setUp() {
        service =
                new RefundCompletedNotificationService(
                        new NotificationDispatcher(
                                notificationRepository, messageHistoryRepository, smsSender),
                        new NotificationRecipientReader(recipientMapper),
                        new RefundCompletedMessageComposer());

        when(notificationRepository.save(any()))
                .thenAnswer(call -> call.<Notification>getArgument(0));
        when(smsSender.send(any(), any())).thenReturn(MessageSendResult.accepted(null, null, null));
    }

    private void givenRecipient(Long userId, String phone) {
        when(recipientMapper.findByTicketOrderId(ORDER_ID))
                .thenReturn(new NotificationRecipient(userId, phone));
    }

    private RefundCompletedEvent event(String reason) {
        return new RefundCompletedEvent(ORDER_ID, ORDER_NUMBER, new BigDecimal("38000"), reason);
    }

    private Notification saved() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        return captor.getValue();
    }

    private String sentText() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(smsSender).send(any(), captor.capture());
        return captor.getValue();
    }

    @Test
    void sendsAndRecordsWithRefundTemplate() {
        givenRecipient(7L, "01012345678");

        service.notifyRefundCompleted(event("단순 변심"));

        Notification notification = saved();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getTemplateCode()).isEqualTo("REFUND_COMPLETED");
        assertThat(notification.getReferenceId()).isEqualTo(ORDER_ID);
        assertThat(notification.getRecipientUserId()).isEqualTo(7L);
    }

    /** 금액은 사람이 읽는 형식이어야 한다. 38000 을 그대로 보내면 자릿수를 세게 된다. */
    @Test
    void smsTextCarriesOrderNumberAndFormattedAmount() {
        givenRecipient(7L, "01012345678");

        service.notifyRefundCompleted(event(null));

        assertThat(sentText()).contains(ORDER_NUMBER).contains("38,000원");
    }

    /** 사유가 없으면 그 줄을 아예 빼야 한다. "사유 null" 이 나가면 안 된다. */
    @Test
    void omitsReasonLineWhenAbsent() {
        givenRecipient(7L, "01012345678");

        service.notifyRefundCompleted(event(null));

        assertThat(sentText()).doesNotContain("사유").doesNotContain("null");
    }

    @Test
    void includesReasonLineWhenPresent() {
        givenRecipient(7L, "01012345678");

        service.notifyRefundCompleted(event("행사 일정 변경"));

        assertThat(sentText()).contains("사유 행사 일정 변경");
    }

    /**
     * <b>사유는 자유 입력이다.</b> 따옴표가 들어와도 payload 가 깨지면 안 된다.
     *
     * <p>{@code notifications.payload} 가 JSONB 라, 깨진 JSON 을 넣으면 INSERT 자체가 실패해 알림이
     * 통째로 사라진다.
     */
    @Test
    void payloadStaysValidJsonWhenReasonHasQuotes() throws Exception {
        givenRecipient(7L, "01012345678");

        service.notifyRefundCompleted(event("고객이 \"환불\" 요청\n두 줄"));

        JsonNode parsed = objectMapper.readTree(saved().getPayload());
        assertThat(parsed.get("orderNumber").asText()).isEqualTo(ORDER_NUMBER);
        assertThat(parsed.get("reason").asText()).contains("환불");
    }

    /** 금액은 숫자로 저장한다. 문자열로 넣으면 나중에 집계할 때 캐스팅해야 한다. */
    @Test
    void payloadKeepsAmountAsNumber() throws Exception {
        givenRecipient(7L, "01012345678");

        service.notifyRefundCompleted(event(null));

        JsonNode parsed = objectMapper.readTree(saved().getPayload());
        assertThat(parsed.get("refundAmount").isNumber()).isTrue();
        assertThat(parsed.get("refundAmount").decimalValue()).isEqualByComparingTo("38000");
    }

    /** 수신번호가 없으면 시도하지 않고 CANCELED 로 남긴다. */
    @Test
    void recordsCanceledWhenRecipientHasNoPhone() {
        givenRecipient(7L, null);

        service.notifyRefundCompleted(event(null));

        assertThat(saved().getStatus()).isEqualTo(NotificationStatus.CANCELED);
        verify(smsSender, never()).send(any(), any());
        verify(messageHistoryRepository, never()).save(any());
    }

    /**
     * 주문을 못 찾은 경우. <b>예외를 던지지 않는다.</b>
     *
     * <p>알림은 이미 끝난 환불에 딸려 오는 후처리라, 여기서 터지면 원인을 찾기 어려운 곳에서 예외가 올라온다.
     * 못 보낸 사실은 {@code CANCELED} 로 남는다.
     */
    @Test
    void recordsCanceledWhenOrderIsMissing() {
        when(recipientMapper.findByTicketOrderId(ORDER_ID)).thenReturn(null);

        service.notifyRefundCompleted(event(null));

        assertThat(saved().getStatus()).isEqualTo(NotificationStatus.CANCELED);
        verify(smsSender, never()).send(any(), any());
    }

    /** 비회원 환불. recipient_user_id 는 비고 번호만 있다. */
    @Test
    void handlesGuestOrder() {
        givenRecipient(null, "01099998888");

        service.notifyRefundCompleted(event(null));

        Notification notification = saved();
        assertThat(notification.getRecipientUserId()).isNull();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }
}
