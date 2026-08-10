package com.expo.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.checkin.dto.TicketIssuanceOrder;
import com.expo.checkin.dto.TicketIssuanceOrderItem;
import com.expo.checkin.dto.TicketIssueResult;
import com.expo.checkin.entity.IssuedTicket;
import com.expo.checkin.entity.TicketAccessToken;
import com.expo.checkin.repository.IssuedTicketRepository;
import com.expo.checkin.repository.TicketAccessTokenRepository;
import com.expo.checkin.repository.TicketIssuanceMapper;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * 발권 규칙을 DB 없이 못박는다.
 *
 * <p>확인하는 것은 넷이다 — 수량만큼 발급되는가, 결제 안 된 주문을 막는가, 중복 호출을 막는가, 토큰 원문이 저장되지 않는가.
 */
class TicketIssueServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final Instant EXPO_END_AT = Instant.parse("2026-09-10T10:00:00Z");

    private final TicketIssuanceMapper mapper = Mockito.mock(TicketIssuanceMapper.class);
    private final IssuedTicketRepository issuedTicketRepository =
            Mockito.mock(IssuedTicketRepository.class);
    private final TicketAccessTokenRepository accessTokenRepository =
            Mockito.mock(TicketAccessTokenRepository.class);

    private TicketIssueService service;

    @BeforeEach
    void setUp() {
        QrTokenProperties properties = new QrTokenProperties();
        properties.setTokenSecret("test-qr-secret-value-for-unit-test-only");

        service =
                new TicketIssueService(
                        mapper,
                        issuedTicketRepository,
                        accessTokenRepository,
                        new TicketCodeGenerator(),
                        new QrTokenGenerator(properties),
                        new AccessTokenGenerator(),
                        new TokenHasher());

        // 시퀀스는 부를 때마다 다른 값을 준다. 실제 DB 시퀀스와 같은 성질이다.
        AtomicLong sequence = new AtomicLong();
        when(mapper.nextTicketCodeSequence()).thenAnswer(call -> sequence.incrementAndGet());

        // saveAll 은 저장된 엔티티를 그대로 돌려준다. ID 는 DB 가 채우므로 여기서는 검증하지 않는다.
        when(issuedTicketRepository.saveAll(any()))
                .thenAnswer(call -> List.copyOf(call.getArgument(0)));
    }

    private void givenOrder(String status, String phoneNumber) {
        when(mapper.findOrderForIssuance(ORDER_ID))
                .thenReturn(
                        new TicketIssuanceOrder(
                                ORDER_ID, "ORD-20260807-0001", status, phoneNumber, EXPO_END_AT));
    }

    private void givenItems(TicketIssuanceOrderItem... items) {
        when(mapper.findOrderItems(ORDER_ID)).thenReturn(List.of(items));
    }

    /** 발권 매수는 항목 수가 아니라 <b>수량의 합</b>이다. 2매 주문이면 티켓이 2장 나와야 한다. */
    @Test
    void issuesOneTicketPerPurchasedQuantity() {
        givenOrder("PAID", "01012345678");
        givenItems(new TicketIssuanceOrderItem(10L, 100L, 2));

        service.issue(ORDER_ID);

        assertThat(capturedTickets()).hasSize(2);
    }

    /** 항목이 여러 개면 각 항목의 수량을 모두 합친다. */
    @Test
    void sumsQuantityAcrossItems() {
        givenOrder("PAID", "01012345678");
        givenItems(
                new TicketIssuanceOrderItem(10L, 100L, 2),
                new TicketIssuanceOrderItem(11L, 100L, 1));

        service.issue(ORDER_ID);

        assertThat(capturedTickets()).hasSize(3);
    }

    /** 같은 주문의 티켓이라도 코드와 QR 해시는 전부 달라야 한다. 둘 다 UNIQUE 컬럼이다. */
    @Test
    void everyTicketGetsItsOwnCodeAndQrHash() {
        givenOrder("PAID", "01012345678");
        givenItems(new TicketIssuanceOrderItem(10L, 100L, 3));

        service.issue(ORDER_ID);

        List<IssuedTicket> tickets = capturedTickets();
        assertThat(tickets).extracting(IssuedTicket::getTicketCode).doesNotHaveDuplicates();
        assertThat(tickets).extracting(IssuedTicket::getQrTokenHash).doesNotHaveDuplicates();
    }

    /** 결제 웹훅은 재시도된다. 두 번 불려도 티켓이 두 배가 되면 안 된다. */
    @Test
    void rejectsSecondIssuanceForSameOrder() {
        givenOrder("PAID", "01012345678");
        when(mapper.countIssuedTickets(ORDER_ID)).thenReturn(2);

        assertThatThrownBy(() -> service.issue(ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_ALREADY_ISSUED);

        verify(issuedTicketRepository, never()).saveAll(any());
    }

    @Test
    void rejectsUnpaidOrder() {
        givenOrder("PENDING", "01012345678");

        assertThatThrownBy(() -> service.issue(ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_ORDER_NOT_PAID);
    }

    @Test
    void rejectsUnknownOrder() {
        when(mapper.findOrderForIssuance(anyLong())).thenReturn(null);

        assertThatThrownBy(() -> service.issue(ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_ORDER_NOT_FOUND);
    }

    @Test
    void rejectsOrderWithoutItems() {
        givenOrder("PAID", "01012345678");
        givenItems();

        assertThatThrownBy(() -> service.issue(ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_ORDER_HAS_NO_ITEM);
    }

    /**
     * 접근 토큰은 <b>원문이 아니라 해시</b>가 저장돼야 한다.
     *
     * <p>원문이 DB 에 들어가면 DB 유출만으로 남의 티켓 링크가 열린다. 결과에 담겨 나오는 원문과 저장된 값이 달라야 한다.
     */
    @Test
    void storesOnlyTheHashOfTheAccessToken() {
        givenOrder("PAID", "01012345678");
        givenItems(new TicketIssuanceOrderItem(10L, 100L, 1));

        TicketIssueResult result = service.issue(ORDER_ID);

        ArgumentCaptor<TicketAccessToken> captor = ArgumentCaptor.forClass(TicketAccessToken.class);
        verify(accessTokenRepository).save(captor.capture());

        assertThat(result.accessTokenValue()).isNotBlank();
        assertThat(captor.getValue().getTokenHash())
                .isNotEqualTo(result.accessTokenValue())
                .isEqualTo(new TokenHasher().hash(result.accessTokenValue()));
    }

    /** 링크는 행사 당일 이후에도 살아 있어야 한다. 종료 시각에 딱 맞추면 마지막 날 저녁에 죽는다. */
    @Test
    void accessTokenOutlivesTheExpo() {
        givenOrder("PAID", "01012345678");
        givenItems(new TicketIssuanceOrderItem(10L, 100L, 1));

        service.issue(ORDER_ID);

        ArgumentCaptor<TicketAccessToken> captor = ArgumentCaptor.forClass(TicketAccessToken.class);
        verify(accessTokenRepository).save(captor.capture());

        assertThat(captor.getValue().getExpiresAt()).isAfter(EXPO_END_AT);
    }

    /** 소셜 로그인 회원은 번호가 없을 수 있다. 그래도 발권 자체는 성공해야 한다 — 마이페이지에서 QR 을 볼 수 있다. */
    @Test
    void issuesEvenWhenRecipientPhoneIsMissing() {
        givenOrder("PAID", null);
        givenItems(new TicketIssuanceOrderItem(10L, 100L, 1));

        TicketIssueResult result = service.issue(ORDER_ID);

        assertThat(result.issuedCount()).isEqualTo(1);
        assertThat(result.hasRecipient()).isFalse();
    }

    @SuppressWarnings("unchecked")
    private List<IssuedTicket> capturedTickets() {
        ArgumentCaptor<List<IssuedTicket>> captor = ArgumentCaptor.forClass(List.class);
        verify(issuedTicketRepository).saveAll(captor.capture());
        return captor.getValue();
    }
}
