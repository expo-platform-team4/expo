package com.expo.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.expo.checkin.repository.CheckInQueryMapper;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * 조회 쪽 규칙을 못박는다.
 *
 * <p>핵심은 <b>페이지 값이 이상해도 500 이 나가지 않는가</b>와 <b>주최자가 아니면 아무것도 안 읽는가</b>이다.
 */
class CheckInQueryServiceTest {

    private static final Long EXPO_ID = 1L;
    private static final Long HOST_CLIENT_ID = 7L;

    private final CheckInQueryMapper mapper = Mockito.mock(CheckInQueryMapper.class);
    private final ExpoHostVerifier expoHostVerifier = Mockito.mock(ExpoHostVerifier.class);

    private final CheckInQueryService service = new CheckInQueryService(mapper, expoHostVerifier);

    private long capturedOffset() {
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(mapper).findHistory(eq(EXPO_ID), anyInt(), captor.capture());
        return captor.getValue();
    }

    /**
     * <b>{@code page * size} 가 int 를 넘겨도 음수가 되면 안 된다.</b>
     *
     * <p>{@code int} 로 곱하면 {@code 2147483647 * 100} 이 {@code -100} 이 되고, PostgreSQL 이
     * {@code OFFSET must not be negative} 로 거절해 조회가 500 으로 떨어진다.
     */
    @Test
    void offsetNeverGoesNegativeOnHugePage() {
        service.history(EXPO_ID, HOST_CLIENT_ID, Integer.MAX_VALUE, 100);

        assertThat(capturedOffset()).isNotNegative().isEqualTo(214748364700L);
    }

    /** 음수 페이지는 첫 페이지로 본다. */
    @Test
    void negativePageBecomesFirstPage() {
        service.history(EXPO_ID, HOST_CLIENT_ID, -5, 20);

        assertThat(capturedOffset()).isZero();
    }

    /** 페이지 크기는 상한을 넘지 못한다. 한 번에 다 퍼가지 못하게 막는다. */
    @Test
    void pageSizeIsClamped() {
        service.history(EXPO_ID, HOST_CLIENT_ID, 0, 100_000);

        ArgumentCaptor<Integer> size = ArgumentCaptor.forClass(Integer.class);
        verify(mapper).findHistory(eq(EXPO_ID), size.capture(), anyLong());
        assertThat(size.getValue()).isEqualTo(100);
    }

    /** 0 이하 크기도 막는다. LIMIT 0 이면 아무것도 안 나온다. */
    @Test
    void zeroPageSizeBecomesOne() {
        service.history(EXPO_ID, HOST_CLIENT_ID, 0, 0);

        ArgumentCaptor<Integer> size = ArgumentCaptor.forClass(Integer.class);
        verify(mapper).findHistory(eq(EXPO_ID), size.capture(), anyLong());
        assertThat(size.getValue()).isEqualTo(1);
    }

    /** 주최자가 아니면 이력을 한 줄도 읽으면 안 된다. */
    @Test
    void nonHostReadsNothingFromHistory() {
        doThrow(new BusinessException(ErrorCode.NOT_EXPO_HOST))
                .when(expoHostVerifier)
                .verifyHost(EXPO_ID, 999L);

        assertThatThrownBy(() -> service.history(EXPO_ID, 999L, 0, 20))
                .isInstanceOf(BusinessException.class);

        verify(mapper, never()).findHistory(anyLong(), anyInt(), anyLong());
        verify(mapper, never()).countHistory(anyLong());
    }

    /** 현황도 마찬가지다. */
    @Test
    void nonHostReadsNothingFromSummary() {
        doThrow(new BusinessException(ErrorCode.NOT_EXPO_HOST))
                .when(expoHostVerifier)
                .verifyHost(EXPO_ID, 999L);

        assertThatThrownBy(() -> service.summary(EXPO_ID, 999L))
                .isInstanceOf(BusinessException.class);

        verify(mapper, never()).summarize(anyLong());
    }
}
