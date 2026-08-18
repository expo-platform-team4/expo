package com.expo.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.expo.event.ExpoCanceledEvent;
import com.expo.notification.dto.ExpoCancelTarget;
import com.expo.notification.dto.NotificationRequest;
import com.expo.notification.repository.NotificationRecipientMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * 박람회 취소 안내를 못박는다.
 *
 * <p>앞의 알림들과 다른 셋에 집중한다 — <b>한 번만 나가는가</b>, <b>대상 전원에게 한 요청으로 가는가</b>,
 * <b>박람회명·사유가 자유 입력이라 payload 가 깨지지 않는가</b>.
 */
class ExpoCanceledNotificationServiceTest {

    private static final Long EXPO_ID = 9L;
    private static final String TITLE = "2026 서울 국제 도서전";

    private final OneShotNotificationGuard guard = Mockito.mock(OneShotNotificationGuard.class);
    private final NotificationRecipientMapper recipientMapper =
            Mockito.mock(NotificationRecipientMapper.class);
    private final NotificationDispatcher dispatcher = Mockito.mock(NotificationDispatcher.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ExpoCanceledNotificationService service;

    @BeforeEach
    void setUp() {
        service =
                new ExpoCanceledNotificationService(
                        guard, recipientMapper, dispatcher, new ExpoCanceledMessageComposer());
        when(guard.claim(anyLong(), any(), any())).thenReturn(true);
    }

    private void givenTargets(int count) {
        List<ExpoCancelTarget> targets =
                java.util.stream.IntStream.range(0, count)
                        .mapToObj(
                                i ->
                                        new ExpoCancelTarget(
                                                1L + i, "0101111%04d".formatted(i), 1L + i))
                        .toList();
        when(recipientMapper.findExpoCancelTargets(EXPO_ID)).thenReturn(targets);
    }

    private ExpoCanceledEvent event(String reason) {
        return new ExpoCanceledEvent(EXPO_ID, TITLE, reason);
    }

    @SuppressWarnings("unchecked")
    private List<NotificationRequest> dispatched() {
        ArgumentCaptor<List<NotificationRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(dispatcher).dispatchMany(captor.capture());
        return captor.getValue();
    }

    /** 대상 전원이 <b>한 번의 dispatchMany</b> 로 나가야 한다. 낱건 반복이면 안 된다. */
    @Test
    void dispatchesEveryoneInOneCall() {
        givenTargets(3);

        service.notifyExpoCanceled(event(null));

        assertThat(dispatched()).hasSize(3);
        verify(dispatcher, never()).dispatch(any());
    }

    /** 이미 보낸 박람회면 대상 조회조차 하지 않는다. */
    @Test
    void doesNothingWhenAlreadySent() {
        when(guard.claim(anyLong(), any(), any())).thenReturn(false);

        service.notifyExpoCanceled(event(null));

        verify(recipientMapper, never()).findExpoCancelTargets(anyLong());
        verify(dispatcher, never()).dispatchMany(any());
    }

    /** 대상이 없으면 발송을 시도하지 않는다. 판 티켓이 없거나 전부 환불된 박람회다. */
    @Test
    void doesNotDispatchWhenNoTargets() {
        when(recipientMapper.findExpoCancelTargets(EXPO_ID)).thenReturn(List.of());

        service.notifyExpoCanceled(event(null));

        verify(dispatcher, never()).dispatchMany(any());
    }

    /** 참조는 주문이 아니라 <b>박람회</b>다. 수신자마다 1행이라 주문으로 묶을 수 없다. */
    @Test
    void referencesTheExpoNotTheOrder() {
        givenTargets(2);

        service.notifyExpoCanceled(event(null));

        assertThat(dispatched())
                .allSatisfy(
                        r -> {
                            assertThat(r.templateCode()).isEqualTo("EXPO_CANCELED");
                            assertThat(r.referenceType()).isEqualTo("EXPO");
                            assertThat(r.referenceId()).isEqualTo(EXPO_ID);
                        });
    }

    /** 본문에 박람회명과 환불 안내가 있어야 한다. 링크·주문번호는 없다. */
    @Test
    void smsTextNamesTheExpoAndPromisesRefund() {
        givenTargets(1);

        service.notifyExpoCanceled(event(null));

        String text = dispatched().get(0).smsText();
        assertThat(text).contains(TITLE).contains("전액 환불").doesNotContain("http");
    }

    /** 사유가 없으면 그 줄을 뺀다. "사유 null" 이 나가면 안 된다. */
    @Test
    void omitsReasonWhenAbsent() {
        givenTargets(1);

        service.notifyExpoCanceled(event(null));

        assertThat(dispatched().get(0).smsText()).doesNotContain("사유").doesNotContain("null");
    }

    @Test
    void includesReasonWhenPresent() {
        givenTargets(1);

        service.notifyExpoCanceled(event("주최사 사정"));

        assertThat(dispatched().get(0).smsText()).contains("사유 주최사 사정");
    }

    /**
     * <b>박람회명·사유는 사람이 입력한 값이다.</b> 따옴표가 들어와도 payload 가 깨지면 안 된다.
     *
     * <p>{@code notifications.payload} 가 JSONB 라, 깨지면 INSERT 가 실패해 알림이 통째로 사라진다.
     */
    @Test
    void payloadStaysValidJsonWithQuotes() throws Exception {
        when(recipientMapper.findExpoCancelTargets(EXPO_ID))
                .thenReturn(List.of(new ExpoCancelTarget(1L, "01011112222", 1L)));

        service.notifyExpoCanceled(new ExpoCanceledEvent(EXPO_ID, "\"특별\" 박람회", "천재지변\n불가항력"));

        var parsed = objectMapper.readTree(dispatched().get(0).payload());
        assertThat(parsed.get("expoTitle").asText()).contains("특별");
        assertThat(parsed.get("reason").asText()).contains("천재지변");
        assertThat(parsed.get("expoId").asLong()).isEqualTo(EXPO_ID);
    }

    /** 문구와 payload 는 대상마다 같아야 한다. 사람마다 다시 만들 이유가 없다. */
    @Test
    void reusesTheSameTextForEveryTarget() {
        givenTargets(3);

        service.notifyExpoCanceled(event("사정"));

        List<NotificationRequest> requests = dispatched();
        assertThat(requests)
                .extracting(NotificationRequest::smsText)
                .containsOnly(requests.get(0).smsText());
        assertThat(requests)
                .extracting(NotificationRequest::payload)
                .containsOnly(requests.get(0).payload());
    }

    /** 번호 없는 대상도 목록에 남는다. 거르는 일은 발송기가 하고 CANCELED 로 기록한다. */
    @Test
    void keepsUnreachableTargetsInTheList() {
        when(recipientMapper.findExpoCancelTargets(EXPO_ID))
                .thenReturn(
                        List.of(
                                new ExpoCancelTarget(1L, "01011112222", 1L),
                                new ExpoCancelTarget(2L, null, 2L)));

        service.notifyExpoCanceled(event(null));

        assertThat(dispatched()).hasSize(2);
        assertThat(dispatched().get(1).recipient().reachable()).isFalse();
    }
}
