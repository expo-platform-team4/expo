package com.expo.notification.dto;

import com.expo.notification.entity.NotificationChannel;

/**
 * 알림 한 건을 보내 달라는 요청.
 *
 * <p>알림 종류마다 다른 것은 <b>이 다섯뿐</b>이고, 보내고 기록하는 절차는 전부 같다. 그래서 종류별 서비스는 이 값을 만들기만 하고 실제 발송은 {@link
 * com.expo.notification.service.NotificationDispatcher} 에 맡긴다.
 *
 * @param recipient 수신자. 번호가 없을 수 있다 — 그 처리는 발송기가 한다
 * @param templateCode {@code TICKET_ISSUED}, {@code REFUND_COMPLETED} 등. {@code
 *     notifications.template_code} 에 그대로 들어간다
 * @param referenceType 무엇에 딸린 알림인지. {@code ORDER}, {@code REFUND} 등
 * @param referenceId 그 대상의 ID
 * @param payload 템플릿 변수(JSON). <b>토큰·비밀번호를 넣지 않는다</b> — 저장되는 값이다
 * @param smsText 실제로 나갈 본문. payload 와 달리 <b>링크가 들어가도 된다</b> (저장되지 않는다)
 */
public record NotificationRequest(
        NotificationRecipient recipient,
        String templateCode,
        String referenceType,
        Long referenceId,
        String payload,
        String smsText) {

    /** 지금은 SMS 뿐이다. 알림톡·이메일이 붙으면 이 값을 요청에 담게 된다. */
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }
}
