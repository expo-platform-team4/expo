package com.expo.checkin.service;

import com.expo.checkin.dto.TicketIssuanceOrder;
import com.expo.checkin.dto.TicketIssuanceOrderItem;
import com.expo.checkin.dto.TicketIssueResult;
import com.expo.checkin.entity.IssuedTicket;
import com.expo.checkin.entity.TicketAccessToken;
import com.expo.checkin.event.TicketIssuedEvent;
import com.expo.checkin.repository.IssuedTicketRepository;
import com.expo.checkin.repository.TicketAccessTokenRepository;
import com.expo.checkin.repository.TicketIssuanceMapper;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제가 끝난 주문에 대해 입장권을 발급한다.
 *
 * <h2>이 클래스가 order·payment 도메인과 만나는 지점이다</h2>
 *
 * 결제 승인에 성공하면 <b>{@link #issue(Long)} 하나만 호출</b>하면 된다. 주문 정보는 이 안에서 직접 읽으므로 order 도메인 클래스를 넘길 필요가
 * 없고, 그쪽 설계가 바뀌어도 호출부는 그대로다.
 *
 * <h2>트랜잭션 경계</h2>
 *
 * 발권은 결제 성공 처리와 <b>같은 트랜잭션</b>에서 끝나야 한다. 재고 확정과 원자적이어야 하기 때문이다. 반면 SMS 발송은 외부 HTTP 라 이 안에서 하지
 * 않는다 — 대행사가 느릴 때 DB 커넥션이 잠긴다. 발송은 커밋 후에 {@link TicketIssueResult} 를 받아 별도로 처리한다.
 *
 * <h2>중복 호출</h2>
 *
 * 결제 웹훅은 재시도된다. 같은 주문으로 두 번 불리면 티켓이 두 배로 발급되므로 {@link ErrorCode#TICKET_ALREADY_ISSUED} 로 막는다.
 */
@Slf4j
@Service
public class TicketIssueService {

    /** 주문 상태가 이 값일 때만 발권한다. {@code ticket_orders.status} 의 CHECK 값이다. */
    private static final String ORDER_STATUS_PAID = "PAID";

    /**
     * 접근 토큰을 박람회 종료 시각보다 얼마나 더 살려 둘지.
     *
     * <p>종료 시각에 딱 맞추면 행사 마지막 날 저녁에 링크가 죽는다. 입장 후 영수증처럼 다시 열어보는 경우가 있어 하루 여유를 둔다.
     */
    private static final Duration ACCESS_TOKEN_GRACE = Duration.ofDays(1);

    private final TicketIssuanceMapper ticketIssuanceMapper;
    private final IssuedTicketRepository issuedTicketRepository;
    private final TicketAccessTokenRepository ticketAccessTokenRepository;
    private final TicketCodeGenerator ticketCodeGenerator;
    private final QrTokenGenerator qrTokenGenerator;
    private final AccessTokenGenerator accessTokenGenerator;
    private final TokenHasher tokenHasher;
    private final ApplicationEventPublisher eventPublisher;

    public TicketIssueService(
            TicketIssuanceMapper ticketIssuanceMapper,
            IssuedTicketRepository issuedTicketRepository,
            TicketAccessTokenRepository ticketAccessTokenRepository,
            TicketCodeGenerator ticketCodeGenerator,
            QrTokenGenerator qrTokenGenerator,
            AccessTokenGenerator accessTokenGenerator,
            TokenHasher tokenHasher,
            ApplicationEventPublisher eventPublisher) {
        this.ticketIssuanceMapper = ticketIssuanceMapper;
        this.issuedTicketRepository = issuedTicketRepository;
        this.ticketAccessTokenRepository = ticketAccessTokenRepository;
        this.ticketCodeGenerator = ticketCodeGenerator;
        this.qrTokenGenerator = qrTokenGenerator;
        this.accessTokenGenerator = accessTokenGenerator;
        this.tokenHasher = tokenHasher;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 주문의 구매 수량만큼 입장권을 발급하고, QR 확인 링크용 접근 토큰을 하나 만든다.
     *
     * @param ticketOrderId 결제가 완료된 {@code ticket_orders.id}
     * @return 발급 결과. 접근 토큰 <b>원문</b>이 여기에만 담겨 나온다
     */
    @Transactional
    public TicketIssueResult issue(Long ticketOrderId) {
        TicketIssuanceOrder order = loadPaidOrder(ticketOrderId);
        List<TicketIssuanceOrderItem> items = loadItems(ticketOrderId);

        Instant issuedAt = Instant.now();
        List<Long> issuedTicketIds = issueTickets(items, issuedAt);
        String accessTokenValue = createAccessToken(order, issuedAt);

        // 수신 번호·토큰 원문은 로그에 남기지 않는다. 개인정보이자 인증 수단이다.
        log.info(
                "발권 완료 orderId={} orderNumber={} count={}",
                order.orderId(),
                order.orderNumber(),
                issuedTicketIds.size());

        TicketIssueResult result =
                new TicketIssueResult(
                        order.orderId(),
                        order.orderNumber(),
                        order.memberUserId(),
                        issuedTicketIds,
                        accessTokenValue,
                        order.recipientPhoneNumber());

        // SMS 발송은 이 트랜잭션이 커밋된 뒤에 일어난다. 리스너가 AFTER_COMMIT 으로 받는다.
        eventPublisher.publishEvent(new TicketIssuedEvent(result));
        return result;
    }

    /**
     * 주문을 읽고 발권해도 되는지 확인한다.
     *
     * <p>순서가 중요하다. {@code findOrderForIssuance} 가 <b>주문 행을 잠그고</b>({@code FOR UPDATE})
     * 돌아온 뒤에 발권 여부를 센다. 중복 발권 방어는 세어 보고 판단하는 check-then-act 인데, 이걸 막아 주는
     * DB 제약이 없어서 잠그지 않으면 두 트랜잭션이 나란히 0 을 보고 양쪽 다 발권한다. 결제 웹훅은 재시도되므로
     * 실무에서 반드시 겪는 경우다.
     */
    private TicketIssuanceOrder loadPaidOrder(Long ticketOrderId) {
        TicketIssuanceOrder order = ticketIssuanceMapper.findOrderForIssuance(ticketOrderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.TICKET_ORDER_NOT_FOUND);
        }
        if (!ORDER_STATUS_PAID.equals(order.status())) {
            throw new BusinessException(ErrorCode.TICKET_ORDER_NOT_PAID);
        }
        if (ticketIssuanceMapper.countIssuedTickets(ticketOrderId) > 0) {
            throw new BusinessException(ErrorCode.TICKET_ALREADY_ISSUED);
        }
        return order;
    }

    private List<TicketIssuanceOrderItem> loadItems(Long ticketOrderId) {
        List<TicketIssuanceOrderItem> items = ticketIssuanceMapper.findOrderItems(ticketOrderId);
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.TICKET_ORDER_HAS_NO_ITEM);
        }
        return items;
    }

    /** 항목마다 {@code quantity} 만큼 입장권을 만든다. 4장을 샀으면 4행이다. */
    private List<Long> issueTickets(List<TicketIssuanceOrderItem> items, Instant issuedAt) {
        List<IssuedTicket> tickets = new ArrayList<>();
        for (TicketIssuanceOrderItem item : items) {
            for (int i = 0; i < item.quantity(); i++) {
                tickets.add(newTicket(item, issuedAt));
            }
        }
        return issuedTicketRepository.saveAll(tickets).stream().map(IssuedTicket::getId).toList();
    }

    private IssuedTicket newTicket(TicketIssuanceOrderItem item, Instant issuedAt) {
        // 일련번호는 DB 시퀀스에서 받는다. 애플리케이션에서 세면 동시 발권에 값이 겹친다.
        long sequence = ticketIssuanceMapper.nextTicketCodeSequence();
        String ticketCode = ticketCodeGenerator.generate(issuedAt, sequence);

        // QR 원문은 저장하지 않는다. 해시만 넣고, 보여줄 때마다 코드로 다시 계산한다.
        String qrTokenHash = tokenHasher.hash(qrTokenGenerator.generatePayload(ticketCode));

        return IssuedTicket.issue(
                item.orderItemId(), item.expoId(), ticketCode, qrTokenHash, issuedAt);
    }

    /**
     * SMS 링크용 접근 토큰을 하나 만든다. 주문 단위라 티켓이 몇 장이든 하나다.
     *
     * @return 토큰 <b>원문</b>. DB 에는 해시만 들어간다
     */
    private String createAccessToken(TicketIssuanceOrder order, Instant issuedAt) {
        String tokenValue = accessTokenGenerator.generate();
        ticketAccessTokenRepository.save(
                TicketAccessToken.forOrder(
                        order.orderId(),
                        tokenHasher.hash(tokenValue),
                        accessTokenExpiry(order, issuedAt)));
        return tokenValue;
    }

    /**
     * 박람회 종료 다음 날까지 살린다.
     *
     * <p>박람회 정보를 못 읽은 경우({@code expoEndAt} 이 {@code null})에도 만료는 NOT NULL 이라 값이 필요하다. 이때는 발권 시각
     * 기준으로 잡는다 — 짧은 쪽으로 틀리는 편이 안전하다.
     */
    private Instant accessTokenExpiry(TicketIssuanceOrder order, Instant issuedAt) {
        Instant base = order.expoEndAt() != null ? order.expoEndAt() : issuedAt;
        return base.plus(ACCESS_TOKEN_GRACE);
    }
}
