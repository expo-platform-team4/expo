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
 * 재발송했다고 링크가 더 오래 살아 있을 이유가 없다. 물려받을 토큰이 없으면 만료를 정할 근거가
 * 없으므로 재발송을 막는다 — 임의로 잡으면 행사 뒤에도 열리는 링크가 생긴다.
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
                        text(payload, "orderNumber"),
                        notification.getRecipientUserId(),
                        // 매수만 쓰이므로 ID 목록은 개수만 맞춘 빈 값이면 된다.
                        Collections.nCopies(ticketCount(payload), 0L),
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

        Instant expiresAt = alive.get(0).getExpiresAt();
        Instant now = Instant.now();
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

    private int ticketCount(JsonNode payload) {
        JsonNode value = payload.get("ticketCount");
        return value == null || value.isNull() ? 0 : value.asInt();
    }
}
