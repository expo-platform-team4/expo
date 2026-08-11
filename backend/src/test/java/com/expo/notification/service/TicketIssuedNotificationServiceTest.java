package com.expo.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.checkin.dto.TicketIssueResult;
import com.expo.notification.dto.MessageSendResult;
import com.expo.notification.entity.MessageHistory;
import com.expo.notification.entity.MessageStatus;
import com.expo.notification.entity.Notification;
import com.expo.notification.entity.NotificationStatus;
import com.expo.notification.repository.MessageHistoryRepository;
import com.expo.notification.repository.NotificationRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * 알림 기록 규칙을 못박는다.
 *
 * <p>가장 중요한 둘은 <b>수신번호가 없어도 조용히 사라지지 않는다</b>는 것과, <b>발송 실패도 이력으로 남는다</b>는 것이다. 둘 다 나중에
 * "왜 문자가 안 왔냐"를 추적할 유일한 근거다.
 */
class TicketIssuedNotificationServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final Long MEMBER_USER_ID = 7L;

    private final NotificationRepository notificationRepository =
            Mockito.mock(NotificationRepository.class);
    private final MessageHistoryRepository messageHistoryRepository =
            Mockito.mock(MessageHistoryRepository.class);
    private final SmsSender smsSender = Mockito.mock(SmsSender.class);

    private TicketIssuedNotificationService service;

    @BeforeEach
    void setUp() {
        service =
                new TicketIssuedNotificationService(
                        notificationRepository,
                        messageHistoryRepository,
                        smsSender,
                        new TicketIssuedMessageComposer("http://localhost:3000"));

        // save 는 받은 엔티티를 그대로 돌려준다. ID 는 DB 가 채우므로 여기서는 null 이다.
        when(notificationRepository.save(any()))
                .thenAnswer(call -> call.<Notification>getArgument(0));
    }

    private TicketIssueResult resultWith(String phoneNumber) {
        return new TicketIssueResult(
                ORDER_ID,
                "ORD-20260810-0001",
                MEMBER_USER_ID,
                List.of(11L, 12L),
                "test-access-token-value",
                phoneNumber);
    }

    @Test
    void sendsAndRecordsSuccess() {
        when(smsSender.send(any(), any()))
                .thenReturn(MessageSendResult.accepted("MID-1", "{}", "{\"ok\":true}"));

        service.notifyTicketIssued(resultWith("01012345678"));

        assertThat(savedNotification().getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(savedHistory().getStatus()).isEqualTo(MessageStatus.SENT);
    }

    /**
     * 수신번호가 없으면 발송을 시도하지 않는다. 그래도 <b>기록은 남긴다.</b>
     *
     * <p>{@code FAILED} 가 아니라 {@code CANCELED} 인 이유는 재시도로 해결되는 문제가 아니어서다.
     */
    @Test
    void recordsCanceledWhenThereIsNoPhoneNumber() {
        service.notifyTicketIssued(resultWith(null));

        assertThat(savedNotification().getStatus()).isEqualTo(NotificationStatus.CANCELED);
        assertThat(savedNotification().getLastError()).isNotBlank();
        verify(smsSender, never()).send(any(), any());
        verify(messageHistoryRepository, never()).save(any());
    }

    /** 발송 실패도 이력으로 남아야 한다. 남지 않으면 "왜 문자가 안 왔냐"를 추적할 수 없다. */
    @Test
    void recordsFailureWithReason() {
        when(smsSender.send(any(), any()))
                .thenReturn(MessageSendResult.failed("HTTP_400", "{}", "{\"error\":\"bad\"}"));

        service.notifyTicketIssued(resultWith("01012345678"));

        assertThat(savedNotification().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(savedNotification().getLastError()).isEqualTo("HTTP_400");
        assertThat(savedHistory().getStatus()).isEqualTo(MessageStatus.FAILED);
        assertThat(savedHistory().getErrorCode()).isEqualTo("HTTP_400");
    }

    /**
     * <b>저장되는 payload 에 접근 토큰이 들어가면 안 된다.</b>
     *
     * <p>DB 에는 토큰 해시만 둔다는 설계가 여기서 무너지면, 알림 테이블만 읽어도 남의 티켓 링크를 만들 수 있다.
     */
    @Test
    void neverStoresTheAccessTokenInPayload() {
        when(smsSender.send(any(), any())).thenReturn(MessageSendResult.accepted(null, null, null));

        service.notifyTicketIssued(resultWith("01012345678"));

        assertThat(savedNotification().getPayload()).doesNotContain("test-access-token-value");
    }

    /** 반대로 <b>문자 본문에는</b> 링크가 들어가야 한다. 안 들어가면 받는 사람이 QR 을 볼 방법이 없다. */
    @Test
    void smsTextCarriesTheLink() {
        when(smsSender.send(any(), any())).thenReturn(MessageSendResult.accepted(null, null, null));

        service.notifyTicketIssued(resultWith("01012345678"));

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(smsSender).send(any(), text.capture());
        assertThat(text.getValue())
                .contains("http://localhost:3000/tickets?token=test-access-token-value")
                .contains("ORD-20260810-0001");
    }

    private Notification savedNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        return captor.getValue();
    }

    private MessageHistory savedHistory() {
        ArgumentCaptor<MessageHistory> captor = ArgumentCaptor.forClass(MessageHistory.class);
        verify(messageHistoryRepository).save(captor.capture());
        return captor.getValue();
    }
}
