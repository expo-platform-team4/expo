package com.expo.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.notification.dto.MessageSendResult;
import com.expo.notification.dto.NotificationRecipient;
import com.expo.notification.dto.NotificationRequest;
import com.expo.notification.dto.SmsMessage;
import com.expo.notification.entity.MessageHistory;
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
 * 대량 발송 경로를 못박는다.
 *
 * <p>가장 위험한 것은 <b>짝이 밀리는 것</b>이다. 번호 없는 대상이 섞여 있으면 발송 목록과 알림 목록의
 * 길이가 달라지는데, 잘못 맞추면 <b>엉뚱한 사람의 결과가 남의 알림에 기록된다.</b> 조용히 틀리기 때문에
 * 눈으로는 안 잡힌다.
 */
class NotificationDispatcherManyTest {

    private final NotificationRepository notificationRepository =
            Mockito.mock(NotificationRepository.class);
    private final MessageHistoryRepository messageHistoryRepository =
            Mockito.mock(MessageHistoryRepository.class);
    private final SmsSender smsSender = Mockito.mock(SmsSender.class);

    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher =
                new NotificationDispatcher(
                        notificationRepository, messageHistoryRepository, smsSender);

        // saveAll 은 받은 목록을 그대로 돌려준다. ID 는 DB 가 채우므로 여기서는 null 이다.
        when(notificationRepository.saveAll(any()))
                .thenAnswer(call -> List.copyOf(call.<List<Notification>>getArgument(0)));
        when(notificationRepository.save(any()))
                .thenAnswer(call -> call.<Notification>getArgument(0));
    }

    private NotificationRequest request(String phone) {
        return new NotificationRequest(
                new NotificationRecipient(1L, phone),
                "EXPO_CANCELED",
                "EXPO",
                9L,
                "{}",
                "본문 " + phone);
    }

    private void givenSendResults(MessageSendResult... results) {
        when(smsSender.sendMany(any())).thenReturn(List.of(results));
    }

    @SuppressWarnings("unchecked")
    private List<Notification> savedNotifications() {
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<SmsMessage> sentMessages() {
        ArgumentCaptor<List<SmsMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(smsSender).sendMany(captor.capture());
        return captor.getValue();
    }

    /** 한 요청으로 보낸다. 낱건 반복이면 안 된다. */
    @Test
    void sendsEveryoneInOneRequest() {
        givenSendResults(ok(), ok(), ok());

        dispatcher.dispatchMany(
                List.of(request("01011112222"), request("01033334444"), request("01055556666")));

        assertThat(sentMessages()).hasSize(3);
        verify(smsSender, never()).send(any(), any());
    }

    /** 발송 전에 저장해야 죽어도 PENDING 이 남는다. */
    @Test
    void savesBeforeSending() {
        givenSendResults(ok());

        dispatcher.dispatchMany(List.of(request("01011112222")));

        org.mockito.InOrder order = Mockito.inOrder(notificationRepository, smsSender);
        order.verify(notificationRepository).saveAll(any());
        order.verify(smsSender).sendMany(any());
    }

    /** 부분 실패. 실패한 것만 FAILED 여야 한다. */
    @Test
    void marksOnlyFailedOnesAsFailed() {
        givenSendResults(ok(), MessageSendResult.failed("1026", "{}", "{}"), ok());

        dispatcher.dispatchMany(
                List.of(request("01011112222"), request("01033334444"), request("01055556666")));

        List<Notification> saved = savedNotifications();
        assertThat(saved.get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(saved.get(1).getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(saved.get(1).getLastError()).isEqualTo("1026");
        assertThat(saved.get(2).getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    /**
     * <b>번호 없는 대상이 섞여도 짝이 밀리면 안 된다.</b>
     *
     * <p>가운데 사람에게 번호가 없으면 발송 목록은 2건인데 대상은 3명이다. 그대로 순서를 맞추면
     * 세 번째 사람이 두 번째 결과를 받는다.
     */
    @Test
    void doesNotMisalignWhenUnreachableIsMixedIn() {
        givenSendResults(ok(), MessageSendResult.failed("9999", "{}", "{}"));

        dispatcher.dispatchMany(
                List.of(
                        request("01011112222"), // 발송 → 성공
                        request(null), // 번호 없음 → CANCELED
                        request("01055556666"))); // 발송 → 실패

        // 발송 목록에는 번호 있는 둘만 들어간다.
        assertThat(sentMessages())
                .extracting(SmsMessage::to)
                .containsExactly("01011112222", "01055556666");

        List<Notification> saved = savedNotifications();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(saved.get(1).getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    /** 번호 없는 대상도 기록은 남는다. 빠뜨리면 "보내려 했으나 못 보냈다" 를 알 수 없다. */
    @Test
    void recordsUnreachableSeparately() {
        givenSendResults(ok());

        dispatcher.dispatchMany(List.of(request("01011112222"), request(null)));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.CANCELED);
    }

    /** 전원이 번호 없음이면 발송을 시도하지 않는다. */
    @Test
    void doesNotCallSenderWhenNobodyIsReachable() {
        dispatcher.dispatchMany(List.of(request(null), request(null)));

        verify(smsSender, never()).sendMany(any());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    void doesNothingOnEmptyInput() {
        dispatcher.dispatchMany(List.of());

        verify(smsSender, never()).sendMany(any());
    }

    /** 이력은 발송한 건수만큼 남는다. */
    @Test
    void writesOneHistoryPerSentMessage() {
        givenSendResults(ok(), ok());

        dispatcher.dispatchMany(List.of(request("01011112222"), request("01033334444")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MessageHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(messageHistoryRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    /**
     * 발송기가 계약을 어겨 길이가 다르면 <b>멈춘다.</b>
     *
     * <p>조용히 넘어가면 짝이 밀린 채로 기록되고, 나중에 "왜 이 사람이 실패로 남았지" 를 추적할 수 없다.
     */
    @Test
    void failsLoudlyWhenResultCountDiffers() {
        givenSendResults(ok()); // 2건 보냈는데 1건만 돌아옴

        assertThatThrownBy(
                        () ->
                                dispatcher.dispatchMany(
                                        List.of(request("01011112222"), request("01033334444"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("요청=2")
                .hasMessageContaining("결과=1");
    }

    private MessageSendResult ok() {
        return MessageSendResult.accepted("G4V-GROUP", "{}", "{}");
    }
}
