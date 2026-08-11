package com.expo.notification.service;

import com.expo.checkin.dto.TicketIssueResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 발권 완료 SMS 의 본문과 저장용 payload 를 만든다.
 *
 * <p>문구와 링크 조립을 발송 로직에서 떼어 놓으면 문구만 바꿀 때 발송 흐름을 건드리지 않아도 되고, 이 클래스만 단위 테스트할 수 있다.
 */
@Component
public class TicketIssuedMessageComposer {

    private final String frontBaseUrl;

    public TicketIssuedMessageComposer(@Value("${app.front-base-url}") String frontBaseUrl) {
        this.frontBaseUrl = frontBaseUrl;
    }

    /**
     * SMS 본문. 링크에 <b>접근 토큰 원문</b>이 들어간다.
     *
     * <p>토큰은 그 자체가 인증 수단이라 이 문자열을 로그에 남기면 안 된다.
     */
    public String smsText(TicketIssueResult result) {
        return """
        [expo] 티켓이 발급되었습니다.
        주문번호 %s (%d매)
        QR 확인 %s"""
                .formatted(result.orderNumber(), result.issuedCount(), ticketViewUrl(result));
    }

    /**
     * {@code notifications.payload} 에 저장할 템플릿 변수.
     *
     * <p><b>접근 토큰을 넣지 않는다.</b> DB 에는 해시만 둔다는 설계가 여기서 무너지면 안 된다. 링크는 발송 시점에만 만들어지고 저장되지 않는다.
     */
    public String payloadJson(TicketIssueResult result) {
        return "{\"orderNumber\":\"%s\",\"ticketCount\":%d}"
                .formatted(result.orderNumber(), result.issuedCount());
    }

    private String ticketViewUrl(TicketIssueResult result) {
        return frontBaseUrl + "/tickets?token=" + result.accessTokenValue();
    }
}
