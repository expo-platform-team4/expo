package com.expo.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.checkin.dto.TicketAccessTokenRow;
import com.expo.checkin.dto.TicketViewResponse;
import com.expo.checkin.dto.TicketViewTicket;
import com.expo.checkin.repository.TicketViewMapper;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * 조회 규칙을 DB 없이 못박는다.
 *
 * <p>가장 중요한 둘은 <b>QR 이 발권 때와 같은 값으로 다시 계산되는가</b>와 <b>못 쓰는 토큰을 이유별로 갈라 막는가</b>이다. 앞의 것이 깨지면
 * 화면의 QR 이 현장에서 거부되고, 뒤의 것이 뭉개지면 받는 사람이 재발급을 받아야 할지 알 수 없다.
 */
class TicketViewServiceTest {

    private static final String TOKEN = "test-access-token-value";
    private static final Long TOKEN_ID = 100L;
    private static final Long ORDER_ID = 1L;
    private static final String ORDER_NUMBER = "ORD-20260810-0001";
    private static final String TICKET_CODE = "EXPO-20260810-000004";

    private final TicketViewMapper mapper = Mockito.mock(TicketViewMapper.class);
    private final TokenHasher tokenHasher = new TokenHasher();

    private QrTokenGenerator qrTokenGenerator;
    private TicketViewService service;

    @BeforeEach
    void setUp() {
        QrTokenProperties properties = new QrTokenProperties();
        properties.setTokenSecret("test-qr-secret-value-for-unit-test-only");
        qrTokenGenerator = new QrTokenGenerator(properties);

        service = new TicketViewService(mapper, qrTokenGenerator, tokenHasher);
    }

    private void givenToken(String status, Instant expiresAt) {
        when(mapper.findAccessTokenByHash(tokenHasher.hash(TOKEN)))
                .thenReturn(
                        new TicketAccessTokenRow(
                                TOKEN_ID, ORDER_ID, ORDER_NUMBER, status, expiresAt));
    }

    private void givenTickets(int count) {
        List<TicketViewTicket> tickets =
                java.util.stream.IntStream.range(0, count)
                        .mapToObj(
                                i ->
                                        new TicketViewTicket(
                                                11L + i,
                                                TICKET_CODE.substring(0, TICKET_CODE.length() - 1)
                                                        + i,
                                                "ISSUED",
                                                null,
                                                "2026 서울 국제 도서전",
                                                Instant.parse("2026-09-01T00:00:00Z"),
                                                Instant.parse("2026-09-10T10:00:00Z")))
                        .toList();
        when(mapper.findTicketsByOrderId(ORDER_ID)).thenReturn(tickets);
    }

    private Instant future() {
        return Instant.now().plus(7, ChronoUnit.DAYS);
    }

    /**
     * <b>이 설계의 핵심 주장이다.</b> DB 에 QR 원문이 없는데도 발권 때와 같은 값이 나와야 한다.
     *
     * <p>여기가 깨지면 화면에 뜬 QR 이 현장 스캔에서 거부된다.
     */
    @Test
    void recomputesTheSameQrAsIssuance() {
        givenToken("ACTIVE", future());
        givenTickets(1);

        TicketViewResponse response = service.view(TOKEN);

        // 발권 때 저장한 해시를 만든 것과 똑같은 계산이다.
        assertThat(response.tickets().get(0).qrPayload())
                .isEqualTo(
                        qrTokenGenerator.generatePayload(response.tickets().get(0).ticketCode()));
    }

    /** 몇 번을 조회해도 같은 QR 이어야 한다. 매번 달라지면 저장하지 않는다는 설계가 성립하지 않는다. */
    @Test
    void qrIsStableAcrossRepeatedViews() {
        givenToken("ACTIVE", future());
        givenTickets(1);

        String first = service.view(TOKEN).tickets().get(0).qrPayload();
        String second = service.view(TOKEN).tickets().get(0).qrPayload();

        assertThat(first).isEqualTo(second);
    }

    /** 주문 단위 링크다. 2매를 샀으면 2장이 다 나와야 한다. */
    @Test
    void returnsEveryTicketOfTheOrder() {
        givenToken("ACTIVE", future());
        givenTickets(2);

        TicketViewResponse response = service.view(TOKEN);

        assertThat(response.ticketCount()).isEqualTo(2);
        assertThat(response.tickets()).hasSize(2);
        assertThat(response.orderNumber()).isEqualTo(ORDER_NUMBER);
    }

    /** 링크가 언제 몇 번 열렸는지는 "문자를 못 받았다" 문의를 가릴 유일한 근거다. */
    @Test
    void recordsTheAccess() {
        givenToken("ACTIVE", future());
        givenTickets(1);

        service.view(TOKEN);

        verify(mapper).touchAccessToken(org.mockito.ArgumentMatchers.eq(TOKEN_ID), any());
    }

    @Test
    void rejectsUnknownToken() {
        when(mapper.findAccessTokenByHash(any())).thenReturn(null);

        assertThatThrownBy(() -> service.view(TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_ACCESS_TOKEN_NOT_FOUND);
    }

    /** 빈 토큰으로 DB 를 두드릴 이유가 없다. */
    @Test
    void rejectsBlankTokenWithoutHittingTheDatabase() {
        assertThatThrownBy(() -> service.view("  "))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_ACCESS_TOKEN_NOT_FOUND);

        verify(mapper, never()).findAccessTokenByHash(any());
    }

    /**
     * 상태는 {@code ACTIVE} 인데 기간이 지난 경우.
     *
     * <p>만료를 {@code EXPIRED} 로 바꿔 주는 배치가 없어서 <b>실제로 대부분 이 모양</b>이다. 상태만 보고 통과시키면 만료가 무의미해진다.
     */
    @Test
    void rejectsTokenPastItsExpiryEvenWhenStatusIsActive() {
        givenToken("ACTIVE", Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> service.view(TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_ACCESS_TOKEN_EXPIRED);
    }

    /** 폐기는 만료와 <b>다른 코드</b>여야 한다. 만료는 재발급을 안내할 수 있지만 폐기는 안 된다. */
    @Test
    void rejectsRevokedTokenWithItsOwnCode() {
        givenToken("REVOKED", future());

        assertThatThrownBy(() -> service.view(TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_ACCESS_TOKEN_REVOKED);
    }

    /** 폐기된 토큰은 기간이 남아 있어도 폐기다. 만료 판정이 폐기를 덮어쓰면 안 된다. */
    @Test
    void revokedBeatsExpiredWhenBothApply() {
        givenToken("REVOKED", Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> service.view(TOKEN))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_ACCESS_TOKEN_REVOKED);
    }

    /** 막힌 토큰은 티켓을 읽지도, 접근 기록을 남기지도 않아야 한다. */
    @Test
    void readsNothingWhenTokenIsRejected() {
        givenToken("REVOKED", future());

        assertThatThrownBy(() -> service.view(TOKEN)).isInstanceOf(BusinessException.class);

        verify(mapper, never()).findTicketsByOrderId(anyLong());
        verify(mapper, never()).touchAccessToken(anyLong(), any());
    }
}
