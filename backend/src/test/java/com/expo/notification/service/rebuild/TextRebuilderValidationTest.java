package com.expo.notification.service.rebuild;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.checkin.entity.TicketAccessToken;
import com.expo.checkin.entity.TicketAccessTokenScope;
import com.expo.checkin.entity.TicketAccessTokenStatus;
import com.expo.checkin.repository.TicketAccessTokenRepository;
import com.expo.checkin.service.AccessTokenGenerator;
import com.expo.checkin.service.TokenHasher;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.notification.entity.Notification;
import com.expo.notification.entity.NotificationChannel;
import com.expo.notification.service.ExpoCanceledMessageComposer;
import com.expo.notification.service.RefundCompletedMessageComposer;
import com.expo.notification.service.TicketIssuedMessageComposer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

/**
 * 재구성기가 <b>못 만들 때 만들지 않는지</b> 못박는다.
 *
 * <p>여기서 틀리면 예외가 안 난다. {@code "주문번호 null (0매)"} 같은 문자가 그대로 나가고 알림은
 * {@code SENT} 로 기록된다. <b>실패했다는 사실조차 남지 않는</b> 종류라, 값이 빠졌을 때 막는 것이
 * 값이 있을 때 잘 만드는 것보다 중요하다.
 */
class TextRebuilderValidationTest {

    private static final Long ORDER_ID = 1L;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TicketAccessTokenRepository accessTokenRepository =
            Mockito.mock(TicketAccessTokenRepository.class);
    private final AccessTokenGenerator accessTokenGenerator =
            Mockito.mock(AccessTokenGenerator.class);

    private Notification notification(String templateCode, String payload) {
        return Notification.pending(
                null,
                "01045770340",
                NotificationChannel.SMS,
                templateCode,
                "ORDER",
                ORDER_ID,
                payload);
    }

    private void assertBlocked(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", ErrorCode.NOTIFICATION_TEMPLATE_NOT_REBUILDABLE);
    }

    // --- 박람회 취소 -------------------------------------------------------

    private ExpoCanceledTextRebuilder expoCanceled() {
        return new ExpoCanceledTextRebuilder(
                objectMapper, new ExpoCanceledMessageComposer(new ObjectMapper()));
    }

    /** 박람회명이 없으면 어느 박람회인지 없는 안내가 나간다. */
    @Test
    void expoCanceledRejectsMissingTitle() {
        Notification n = notification("EXPO_CANCELED", "{\"reason\":\"사정\"}");

        assertBlocked(() -> expoCanceled().rebuild(n));
    }

    /** 사유는 원래 없을 수 있다. 본문에서 생략된다. */
    @Test
    void expoCanceledAllowsMissingReason() {
        Notification n = notification("EXPO_CANCELED", "{\"expoTitle\":\"도서전\"}");

        assertThat(expoCanceled().rebuild(n)).contains("도서전");
    }

    // --- 환불 완료 ---------------------------------------------------------

    private RefundCompletedTextRebuilder refundCompleted() {
        return new RefundCompletedTextRebuilder(
                objectMapper, new RefundCompletedMessageComposer(new ObjectMapper()));
    }

    /** 금액이 없으면 "환불금액 0원" 이 나간다. */
    @Test
    void refundCompletedRejectsMissingAmount() {
        Notification n = notification("REFUND_COMPLETED", "{\"orderNumber\":\"ORD-1\"}");

        assertBlocked(() -> refundCompleted().rebuild(n));
    }

    @Test
    void refundCompletedRejectsMissingOrderNumber() {
        Notification n = notification("REFUND_COMPLETED", "{\"refundAmount\":10000}");

        assertBlocked(() -> refundCompleted().rebuild(n));
    }

    // --- 발권 완료 ---------------------------------------------------------

    private TicketIssuedTextRebuilder ticketIssued() {
        return new TicketIssuedTextRebuilder(
                objectMapper,
                new TicketIssuedMessageComposer("http://localhost:3000"),
                accessTokenRepository,
                accessTokenGenerator,
                new TokenHasher());
    }

    private void givenAliveToken(Instant expiresAt) {
        when(accessTokenRepository.findByTicketOrderIdAndScopeAndStatusOrderByIdDesc(
                        ORDER_ID,
                        TicketAccessTokenScope.ORDER_VIEW,
                        TicketAccessTokenStatus.ACTIVE))
                .thenReturn(List.of(TicketAccessToken.forOrder(ORDER_ID, "hash", expiresAt)));
        when(accessTokenGenerator.generate()).thenReturn("new-token-value");
    }

    /** 매수가 0 이면 "(0매)" 짜리 발권 문자가 나간다. */
    @Test
    void ticketIssuedRejectsZeroCount() {
        givenAliveToken(Instant.now().plus(1, ChronoUnit.DAYS));
        Notification n =
                notification("TICKET_ISSUED", "{\"orderNumber\":\"ORD-1\",\"ticketCount\":0}");

        assertBlocked(() -> ticketIssued().rebuild(n));
    }

    /**
     * <b>이미 만료된 토큰의 만료를 물려받지 않는다.</b>
     *
     * <p>{@code status} 가 {@code ACTIVE} 라도 {@code expires_at} 이 지난 행은 남아 있다 — 만료는
     * 시각으로 판정되지 상태 컬럼이 저절로 바뀌지 않는다. 그걸 승계하면 받는 순간 죽어 있는 링크를
     * 보내고 알림은 {@code SENT} 로 남는다.
     */
    @Test
    void ticketIssuedRejectsExpiredTokenInheritance() {
        givenAliveToken(Instant.now().minus(1, ChronoUnit.HOURS));
        Notification n =
                notification("TICKET_ISSUED", "{\"orderNumber\":\"ORD-1\",\"ticketCount\":2}");

        assertBlocked(() -> ticketIssued().rebuild(n));
        verify(accessTokenRepository, never()).save(any());
    }

    /** 살아 있는 토큰이 있으면 만료를 물려받아 새로 발급한다. */
    @Test
    void ticketIssuedInheritsLivingExpiry() {
        Instant expiresAt = Instant.now().plus(3, ChronoUnit.DAYS);
        givenAliveToken(expiresAt);
        Notification n =
                notification("TICKET_ISSUED", "{\"orderNumber\":\"ORD-1\",\"ticketCount\":2}");

        assertThat(ticketIssued().rebuild(n)).contains("ORD-1", "2매", "new-token-value");
        verify(accessTokenRepository).save(any());
    }
}
