package com.expo.notification.service.rebuild;

import com.expo.checkin.dto.TicketIssueResult;
import com.expo.checkin.entity.TicketAccessToken;
import com.expo.checkin.entity.TicketAccessTokenScope;
import com.expo.checkin.entity.TicketAccessTokenStatus;
import com.expo.checkin.repository.TicketAccessTokenRepository;
import com.expo.checkin.service.AccessTokenGenerator;
import com.expo.checkin.service.TokenHasher;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.notification.entity.Notification;
import com.expo.notification.service.TicketIssuedMessageComposer;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 발권 완료 문자를 다시 만든다. <b>셋 중 유일하게 "복원" 이 아니라 "재발급" 이다.</b>
 *
 * <h2>왜 원래 문자를 되살릴 수 없나</h2>
 *
 * 본문에 QR 확인 링크가 들어가는데, 그 링크의 접근 토큰 <b>원문이 DB 어디에도 없다.</b>
 * {@code ticket_access_tokens} 는 SHA-256 해시만 저장한다 — DB 가 통째로 유출돼도 남의 티켓을 열지
 * 못하게 하려는 설계의 핵심이고, 그 대가로 <b>우리도 원문을 되찾을 수 없다.</b>
 *
 * <p>그래서 재발송은 <b>새 토큰을 발급</b>한다. 배달되지 않은 문자라 원래 링크가 쓰인 적도 없으니
 * 새 링크를 주는 것이 이상하지 않다.
 *
 * <h2>이전 링크는 끊는다</h2>
 *
 * 새 토큰만 만들고 옛것을 두면 한 주문에 살아 있는 링크가 여럿이 된다. 폐기 시각으로 추적이 되고,
 * "지금 유효한 링크는 하나" 라는 규칙이 유지된다. 폐기된 링크를 연 사람은 403 을 받고, 만료(410)와
 * 구분되므로 재발급 안내를 잘못 하지 않는다.
 *
 * <h2>만료 시각은 이전 토큰에서 물려받는다</h2>
 *
 * 새로 계산하지 않는다. 만료는 <b>박람회 종료</b>에 매인 값이지 발송 시각에 매인 값이 아니다.
 * 재발송했다고 링크가 더 오래 살아 있을 이유가 없다.
 *
 * <p>물려받는 대상은 <b>아직 안 지난 만료</b>여야 한다. {@code status} 가 {@code ACTIVE} 라도
 * {@code expires_at} 이 지난 행은 남아 있다 — 만료는 시각으로 판정되지 상태 컬럼이 저절로 바뀌지
 * 않는다. 지난 값을 물려받으면 <b>받는 순간 이미 죽어 있는 링크</b>를 보내고 알림은 {@code SENT} 로
 * 남는다. 승계할 만료가 없으면 재발송을 막는다.
 */
@Slf4j
@Component
public class TicketIssuedTextRebuilder implements NotificationTextRebuilder {

    private final ObjectMapper objectMapper;
    private final TicketIssuedMessageComposer messageComposer;
    private final TicketAccessTokenRepository accessTokenRepository;
    private final AccessTokenGenerator accessTokenGenerator;
    private final TokenHasher tokenHasher;

    public TicketIssuedTextRebuilder(
            ObjectMapper objectMapper,
            TicketIssuedMessageComposer messageComposer,
            TicketAccessTokenRepository accessTokenRepository,
            AccessTokenGenerator accessTokenGenerator,
            TokenHasher tokenHasher) {
        this.objectMapper = objectMapper;
        this.messageComposer = messageComposer;
        this.accessTokenRepository = accessTokenRepository;
        this.accessTokenGenerator = accessTokenGenerator;
        this.tokenHasher = tokenHasher;
    }

    @Override
    public String templateCode() {
        return "TICKET_ISSUED";
    }

    @Override
    public String rebuild(Notification notification) {
        Long orderId = notification.getReferenceId();
        if (orderId == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_REBUILDABLE);
        }

        JsonNode payload = parse(notification);
        String newTokenValue = reissueAccessToken(orderId);

        return messageComposer.smsText(
                new TicketIssueResult(
                        orderId,
                        requiredOrderNumber(payload),
                        notification.getRecipientUserId(),
                        // 매수만 쓰이므로 ID 목록은 개수만 맞춘 빈 값이면 된다.
                        Collections.nCopies(requiredTicketCount(payload), 0L),
                        newTokenValue,
                        notification.getRecipientPhoneNumber()));
    }

    /**
     * 이전 링크를 끊고 새 링크를 발급한다.
     *
     * @return 새 토큰 <b>원문</b>. 저장되는 것은 해시뿐이라 이 값은 여기서만 손에 잡힌다
     */
    private String reissueAccessToken(Long orderId) {
        List<TicketAccessToken> alive =
                accessTokenRepository.findByTicketOrderIdAndScopeAndStatusOrderByIdDesc(
                        orderId, TicketAccessTokenScope.ORDER_VIEW, TicketAccessTokenStatus.ACTIVE);

        if (alive.isEmpty()) {
            // 만료 시각을 물려받을 곳이 없다. 임의로 잡으면 행사 뒤에도 열리는 링크가 생긴다.
            throw new BusinessException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_REBUILDABLE);
        }

        Instant now = Instant.now();

        // 물려받을 만료는 <b>아직 살아 있는</b> 토큰의 것이어야 한다.
        // status 가 ACTIVE 라도 expires_at 이 지난 행이 남아 있을 수 있다 — 만료는 시각으로
        // 판정되지 상태 컬럼이 자동으로 바뀌지 않는다. 그걸 그대로 승계하면 이미 죽은 링크를
        // 새 토큰으로 발급해 보내고, 알림은 SENT 로 남는다. 받는 사람만 안 열린다.
        Instant expiresAt =
                alive.stream()
                        .map(TicketAccessToken::getExpiresAt)
                        .filter(now::isBefore)
                        .max(Instant::compareTo)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOTIFICATION_TEMPLATE_NOT_REBUILDABLE));

        // 폐기는 만료 여부와 무관하게 전부 한다. 만료된 것을 또 끊어도 손해가 없고,
        // 남겨 두면 "살아 있는 링크는 하나" 규칙이 흐려진다.
        alive.forEach(token -> token.revoke(now));

        String tokenValue = accessTokenGenerator.generate();
        accessTokenRepository.save(
                TicketAccessToken.forOrder(orderId, tokenHasher.hash(tokenValue), expiresAt));

        // 토큰 원문은 로그에 남기지 않는다. 그 자체가 인증 수단이다.
        log.info("재발송으로 접근 토큰 재발급 orderId={} 폐기={}건", orderId, alive.size());
        return tokenValue;
    }

    private JsonNode parse(Notification notification) {
        try {
            return objectMapper.readTree(notification.getPayload());
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_REBUILDABLE);
        }
    }

    private String text(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    /** 없으면 본문을 만들 수 없는 값. */
    private String requiredOrderNumber(JsonNode payload) {
        String value = text(payload, "orderNumber");
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_REBUILDABLE);
        }
        return value;
    }

    /**
     * 발권 매수. <b>0 이면 보내지 않는다.</b>
     *
     * <p>"(0매)" 라고 적힌 발권 완료 문자는 받는 사람에게 아무 의미가 없고, 링크를 열면 티켓이 있다.
     * payload 가 깨졌다는 신호이므로 막는다.
     */
    private int requiredTicketCount(JsonNode payload) {
        JsonNode value = payload.get("ticketCount");
        int count = value == null || value.isNull() ? 0 : value.asInt();
        if (count <= 0) {
            throw new BusinessException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_REBUILDABLE);
        }
        return count;
    }
}
