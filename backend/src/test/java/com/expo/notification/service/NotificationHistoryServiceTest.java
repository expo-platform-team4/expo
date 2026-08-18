package com.expo.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.notification.dto.NotificationHistoryPage;
import com.expo.notification.dto.NotificationHistoryRow;
import com.expo.notification.repository.NotificationHistoryMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * 관리자 알림 이력 조회의 규칙을 못박는다.
 *
 * <p>여기서 지키는 것은 셋이다 — <b>번호를 가리는가</b>, <b>재발송 가능 판단이 맞는가</b>, <b>페이지 값이 이상해도
 * 안전한가</b>. 앞의 둘은 틀려도 아무 예외가 안 나서 조용히 지나간다.
 */
class NotificationHistoryServiceTest {

    private final NotificationHistoryMapper historyMapper =
            Mockito.mock(NotificationHistoryMapper.class);
    private final NotificationTextRebuilders textRebuilders =
            Mockito.mock(NotificationTextRebuilders.class);

    private NotificationHistoryService service;

    @BeforeEach
    void setUp() {
        service =
                new NotificationHistoryService(
                        historyMapper, new PhoneNumberMasker(), textRebuilders);
        when(textRebuilders.supports("EXPO_CANCELED")).thenReturn(true);
        when(historyMapper.countSearch(any(), any(), any(), any(), any(), any())).thenReturn(1L);
    }

    private NotificationHistoryRow row(String status, String phone) {
        return new NotificationHistoryRow(
                412L,
                37L,
                phone,
                "SMS",
                "EXPO_CANCELED",
                "EXPO",
                9L,
                status,
                0,
                "1026",
                null,
                Instant.parse("2026-08-18T00:00:00Z"),
                1,
                1,
                "FAILED",
                "1026",
                Instant.parse("2026-08-18T00:00:01Z"));
    }

    private void given(NotificationHistoryRow... rows) {
        when(historyMapper.search(any(), any(), any(), any(), any(), any(), anyInt(), anyLong()))
                .thenReturn(List.of(rows));
    }

    private NotificationHistoryPage search() {
        return service.search(null, null, null, null, null, null, 0, 20);
    }

    /** 목록에 원문 번호가 실려 나가면 안 된다. */
    @Test
    void masksRecipientPhoneNumber() {
        given(row("FAILED", "01045770340"));

        assertThat(search().items().get(0).recipientPhoneNumber()).isEqualTo("010****0340");
    }

    /** 번호가 없는 대상도 목록에 나온다. 가릴 것이 없을 뿐이다. */
    @Test
    void keepsNullPhoneNumberAsNull() {
        given(row("CANCELED", null));

        assertThat(search().items().get(0).recipientPhoneNumber()).isNull();
    }

    /** 실패만 재발송할 수 있다. */
    @Test
    void marksOnlyFailedAsRetryable() {
        given(row("FAILED", "01045770340"));

        assertThat(search().items().get(0).retryable()).isTrue();
    }

    /**
     * 이미 나간 것과, 보낼 번호가 없어 시도조차 못 한 것은 재발송 대상이 아니다.
     *
     * <p>{@code CANCELED} 를 재발송 가능으로 두면 관리자가 눌러도 아무 일이 일어나지 않는 버튼이 생긴다.
     */
    @Test
    void marksSentAndCanceledAsNotRetryable() {
        given(row("SENT", "01045770340"));
        assertThat(search().items().get(0).retryable()).isFalse();

        given(row("CANCELED", null));
        assertThat(search().items().get(0).retryable()).isFalse();
    }

    /** 실패했더라도 보낼 번호가 없으면 못 보낸다. 상태만 보면 안 된다. */
    @Test
    void failedWithoutPhoneNumberIsNotRetryable() {
        given(row("FAILED", null));

        assertThat(search().items().get(0).retryable()).isFalse();
    }

    /**
     * 큰 페이지 번호에 오버플로가 나면 안 된다.
     *
     * <p>{@code int} 로 계산하면 {@code 2147483647 * 100} 이 음수가 되고, PostgreSQL 이 {@code OFFSET must
     * not be negative} 로 거절해 500 이 난다. 체크인 이력 조회에서 실제로 밟았던 자리다.
     */
    @Test
    void doesNotOverflowOffsetOnHugePage() {
        given(row("SENT", "01045770340"));

        service.search(null, null, null, null, null, null, Integer.MAX_VALUE, 100);

        verify(historyMapper)
                .search(any(), any(), any(), any(), any(), any(), eq(100), eq(214748364700L));
    }

    /**
     * 7자리 번호는 <b>통째로</b> 가린다.
     *
     * <p>앞 3 + 뒤 4 로 가리면 {@code 0101234} → {@code 010****1234} 가 되는데, 별표만 끼었을 뿐
     * 원본 숫자가 하나도 안 가려진다. 가린 척하고 원문을 내보내는 쪽이 더 나쁘다.
     */
    @Test
    void fullyMasksShortPhoneNumber() {
        given(row("FAILED", "0101234"));

        assertThat(search().items().get(0).recipientPhoneNumber()).isEqualTo("***");
    }

    /**
     * 본문을 다시 만들 수 없는 템플릿은 재발송할 수 없다.
     *
     * <p>여기서 안 걸러 내면 목록은 {@code retryable=true} 인데 재발송 API 가 409 를 낸다 —
     * 눌러도 안 되는 버튼이다.
     */
    @Test
    void unknownTemplateIsNotRetryable() {
        when(textRebuilders.supports("EXPO_CANCELED")).thenReturn(false);
        given(row("FAILED", "01045770340"));

        assertThat(search().items().get(0).retryable()).isFalse();
    }

    /** 페이지 크기는 상한을 넘지 못하고, 음수 페이지는 0 으로 접힌다. */
    @Test
    void clampsPageAndSize() {
        given(row("SENT", "01045770340"));

        NotificationHistoryPage page =
                service.search(null, null, null, null, null, null, -5, 9_999);

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(100);
        verify(historyMapper).search(any(), any(), any(), any(), any(), any(), eq(100), eq(0L));
    }
}
