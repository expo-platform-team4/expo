package com.expo.checkin.dto;

import java.util.List;

/**
 * 발권 결과. 알림 발송(6단계)이 이 값을 받아 SMS 본문과 링크를 만든다.
 *
 * @param orderId 발권한 주문
 * @param orderNumber 주문번호. SMS 본문에 쓴다
 * @param issuedTicketIds 발급된 입장권 ID 목록. 크기가 곧 발권 매수다
 * @param accessTokenValue <b>접근 토큰 원문.</b> DB 에는 해시만 저장되므로 이 값은 여기서만 나온다. 링크에 실어 보내고 나면 다시 얻을 수
 *     없다. <b>로그에 남기지 않는다</b> — 이 토큰이 곧 인증 수단이다
 * @param recipientPhoneNumber 수신 번호. 없을 수 있다({@code null}) — 소셜 로그인 회원은 번호가 없다
 */
public record TicketIssueResult(
        Long orderId,
        String orderNumber,
        List<Long> issuedTicketIds,
        String accessTokenValue,
        String recipientPhoneNumber) {

    /** 발권 매수. */
    public int issuedCount() {
        return issuedTicketIds.size();
    }

    /** SMS 를 보낼 수 있는 주문인지. */
    public boolean hasRecipient() {
        return recipientPhoneNumber != null && !recipientPhoneNumber.isBlank();
    }
}
