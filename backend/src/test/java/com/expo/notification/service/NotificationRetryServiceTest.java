package com.expo.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.notification.dto.MessageSendResult;
import com.expo.notification.dto.NotificationRetryResult;
import com.expo.notification.entity.MessageHistory;
import com.expo.notification.entity.Notification;
import com.expo.notification.entity.NotificationChannel;
import com.expo.notification.repository.MessageHistoryRepository;
import com.expo.notification.repository.NotificationHistoryMapper;
import com.expo.notification.repository.NotificationRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * 재발송의 방어 규칙을 못박는다.
 *
 * <p>여기서 틀리면 <b>같은 사람에게 문자가 두 번 간다.</b> 그래서 "보낼 수 있는가" 를 판단하는 조건 하나하나를
 * 따로 검사한다 — 조건이 하나만 빠져도 전체가 무너지는 종류의 코드다.
 *
 * <p>다만 <b>동시성 자체는 여기서 검증되지 않는다.</b> 잠금은 실제 DB 가 있어야 의미가 있고, 단위 테스트는
 * 단일 스레드다. 이 테스트가 지키는 것은 "잠금으로 읽은 상태를 보고 판단한다" 는 순서뿐이다.
 */
class NotificationRetryServiceTest {

    private static final Long NOTIFICATION_ID = 412L;

    private final NotificationHistoryMapper historyMapper =
            Mockito.mock(NotificationHistoryMapper.class);
    private final NotificationRepository notificationRepository =
            Mockito.mock(NotificationRepository.class);
    private final MessageHistoryRepository messageHistoryRepository =
            Mockito.mock(MessageHistoryRepository.class);
    private final NotificationTextRebuilders textRebuilders =
            Mockito.mock(NotificationTextRebuilders.class);
    private final SmsSender smsSender = Mockito.mock(SmsSender.class);

    private NotificationRetryService service;

    @BeforeEach
    void setUp() {
        service =
                new NotificationRetryService(
                        historyMapper,
                        notificationRepository,
                        messageHistoryRepository,
                        textRebuilders,
                        smsSender);
        when(textRebuilders.rebuild(any())).thenReturn("[expo] 다시 만든 본문");
        when(historyMapper.nextAttemptNo(NOTIFICATION_ID)).thenReturn(2);
    }

    /** 잠금이 돌려준 상태와, 엔티티로 읽은 알림을 함께 준비한다. */
    private Notification given(String lockedStatus, String phoneNumber) {
        Notification notification =
                Notification.pending(
                        37L,
                        phoneNumber,
                        NotificationChannel.SMS,
                        "REFUND_COMPLETED",
                        "ORDER",
                        9L,
                        "{}");
        notification.markFailed("1026");

        when(historyMapper.lockNotification(NOTIFICATION_ID)).thenReturn(lockedStatus);
        when(notificationRepository.findById(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));
        return notification;
    }

    private void givenSendResult(MessageSendResult result) {
        when(smsSender.send(anyString(), anyString())).thenReturn(result);
    }

    private MessageSendResult accepted() {
        return MessageSendResult.accepted("M4V-1", "{}", "{}");
    }

    /** 성공하면 SENT 가 되고, 시도 번호가 붙은 이력이 한 행 남는다. */
    @Test
    void marksSentAndRecordsAttemptOnSuccess() {
        Notification notification = given("FAILED", "01045770340");
        givenSendResult(accepted());

        NotificationRetryResult result = service.retry(NOTIFICATION_ID);

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo("SENT");
        assertThat(result.attemptNo()).isEqualTo(2);
        assertThat(notification.getRetryCount()).isEqualTo(1);

        ArgumentCaptor<MessageHistory> captor = ArgumentCaptor.forClass(MessageHistory.class);
        verify(messageHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getAttemptNo()).isEqualTo(2);
    }

    /**
     * 시도 번호를 세어서 넘긴다. <b>1 로 고정하면 안 된다.</b>
     *
     * <p>{@code uq_message_histories_attempt (notification_id, attempt_no)} 때문에 같은 번호로 두 번
     * 넣으면 INSERT 가 실패한다. 최초 발송이 1 을 썼으므로 재발송은 2 여야 한다.
     */
    @Test
    void usesCountedAttemptNumberNotOne() {
        given("FAILED", "01045770340");
        givenSendResult(accepted());
        when(historyMapper.nextAttemptNo(NOTIFICATION_ID)).thenReturn(5);

        assertThat(service.retry(NOTIFICATION_ID).attemptNo()).isEqualTo(5);
    }

    /** 대행사가 거절해도 예외가 아니다. 결과로 알리고 이력에 남긴다. */
    @Test
    void keepsFailureAsResultNotException() {
        Notification notification = given("FAILED", "01045770340");
        givenSendResult(MessageSendResult.failed("1026", "{}", "{}"));

        NotificationRetryResult result = service.retry(NOTIFICATION_ID);

        assertThat(result.success()).isFalse();
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.errorCode()).isEqualTo("1026");
        assertThat(notification.getRetryCount()).isEqualTo(1);
        verify(messageHistoryRepository).save(any());
    }

    /** 없는 알림. */
    @Test
    void rejectsUnknownNotification() {
        when(historyMapper.lockNotification(NOTIFICATION_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.retry(NOTIFICATION_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    /**
     * 이미 나간 알림은 다시 보내지 않는다.
     *
     * <p><b>판단 근거가 잠금으로 읽은 상태</b>라는 점이 핵심이다. 관리자 둘이 동시에 눌렀을 때 두 번째
     * 요청은 첫 번째가 커밋한 {@code SENT} 를 보고 여기서 걸린다.
     */
    @Test
    void rejectsAlreadySentNotification() {
        given("SENT", "01045770340");

        assertThatThrownBy(() -> service.retry(NOTIFICATION_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_RETRYABLE);

        verify(smsSender, never()).send(anyString(), anyString());
    }

    /** 번호가 없어 시도조차 못 한 알림은 다시 눌러도 결과가 같다. */
    @Test
    void rejectsCanceledNotification() {
        given("CANCELED", null);

        assertThatThrownBy(() -> service.retry(NOTIFICATION_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_RETRYABLE);
    }

    /** 상태는 실패인데 번호가 비었다면, 보낼 곳이 없다는 뜻이다. */
    @Test
    void rejectsFailedNotificationWithoutPhoneNumber() {
        given("FAILED", null);

        assertThatThrownBy(() -> service.retry(NOTIFICATION_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", ErrorCode.NOTIFICATION_RECIPIENT_UNREACHABLE);

        verify(smsSender, never()).send(anyString(), anyString());
    }

    /** 본문을 못 만들면 보내지 않는다. 빈 문자를 보내는 것보다 막는 편이 낫다. */
    @Test
    void doesNotSendWhenTextCannotBeRebuilt() {
        given("FAILED", "01045770340");
        when(textRebuilders.rebuild(any()))
                .thenThrow(new BusinessException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_REBUILDABLE));

        assertThatThrownBy(() -> service.retry(NOTIFICATION_ID))
                .isInstanceOf(BusinessException.class);

        verify(smsSender, never()).send(anyString(), anyString());
        verify(messageHistoryRepository, never()).save(any());
    }
}
