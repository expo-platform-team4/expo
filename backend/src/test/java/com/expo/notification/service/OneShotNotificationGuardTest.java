package com.expo.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.notification.repository.NotificationRecipientMapper;
import com.expo.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

/**
 * 한 번만 보내는 알림의 중복 방어를 못박는다.
 *
 * <p>가장 중요한 것은 <b>잠금이 검사보다 먼저인가</b>이다. 순서가 뒤집히면 방어가 통째로 사라지는데,
 * 뒤집혀도 단일 스레드 테스트는 전부 통과한다 — 그래서 순서 자체를 검증한다.
 */
class OneShotNotificationGuardTest {

    private static final Long EXPO_ID = 1L;
    private static final String TEMPLATE = "EXPO_CANCELED";
    private static final String REF_TYPE = "EXPO";

    private final NotificationRepository notificationRepository =
            Mockito.mock(NotificationRepository.class);
    private final NotificationRecipientMapper recipientMapper =
            Mockito.mock(NotificationRecipientMapper.class);

    private final OneShotNotificationGuard guard =
            new OneShotNotificationGuard(notificationRepository, recipientMapper);

    private void givenAlreadySent(boolean sent) {
        when(notificationRepository.existsByTemplateCodeAndReferenceTypeAndReferenceId(
                        TEMPLATE, REF_TYPE, EXPO_ID))
                .thenReturn(sent);
    }

    @Test
    void allowsFirstDispatch() {
        givenAlreadySent(false);

        assertThat(guard.claim(EXPO_ID, TEMPLATE, REF_TYPE)).isTrue();
    }

    @Test
    void blocksSecondDispatch() {
        givenAlreadySent(true);

        assertThat(guard.claim(EXPO_ID, TEMPLATE, REF_TYPE)).isFalse();
    }

    /**
     * <b>잠금이 검사보다 먼저여야 한다.</b>
     *
     * <p>순서가 뒤집히면 두 요청이 나란히 "아직 안 보냄" 을 읽고 양쪽 다 보낸다. 발권·체크인에서 겪은
     * check-then-act 와 같은 구조다.
     */
    @Test
    void locksBeforeChecking() {
        givenAlreadySent(false);

        guard.claim(EXPO_ID, TEMPLATE, REF_TYPE);

        InOrder order = inOrder(recipientMapper, notificationRepository);
        order.verify(recipientMapper).lockExpo(EXPO_ID);
        order.verify(notificationRepository)
                .existsByTemplateCodeAndReferenceTypeAndReferenceId(TEMPLATE, REF_TYPE, EXPO_ID);
    }

    /** 이미 보낸 경우에도 잠금은 잡는다. 잡지 않으면 검사 자체가 의미 없다. */
    @Test
    void locksEvenWhenAlreadySent() {
        givenAlreadySent(true);

        guard.claim(EXPO_ID, TEMPLATE, REF_TYPE);

        verify(recipientMapper).lockExpo(EXPO_ID);
    }
}
