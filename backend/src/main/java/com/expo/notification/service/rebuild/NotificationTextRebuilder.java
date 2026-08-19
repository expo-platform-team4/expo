package com.expo.notification.service.rebuild;

import com.expo.notification.entity.Notification;

/**
 * 저장된 알림으로 <b>문자 본문을 다시 만든다.</b> 재발송(D-API-010)이 쓴다.
 *
 * <h2>왜 다시 만들어야 하나</h2>
 *
 * 보낸 본문을 저장하지 않기 때문이다. {@code message_histories.request_payload} 에는
 * {@code {"to":..., "from":..., "textLength":47}} 만 남는다 — 본문에 접근 토큰이 들어 있어 저장하지
 * 않기로 한 결정이다.
 *
 * <p>그래서 재발송은 {@code notifications.payload}(템플릿 변수)로 본문을 <b>복원</b>한다. 원문을 그대로
 * 되살리는 것이 아니라 <b>같은 템플릿에 같은 변수를 다시 넣는</b> 것이다.
 *
 * <h2>템플릿마다 사정이 다르다</h2>
 *
 * <pre>
 * REFUND_COMPLETED  payload 에 필요한 값이 다 있다        → 그대로 복원된다
 * EXPO_CANCELED     payload 에 필요한 값이 다 있다        → 그대로 복원된다
 * TICKET_ISSUED     본문에 QR 링크가 들어가는데 그 토큰 원문은 DB 에 없다(해시만 저장)
 *                   → <b>새 토큰을 발급</b>해야 한다. 복원이 아니라 재발급이다
 * </pre>
 *
 * <p>구현을 템플릿별로 나눈 이유가 이것이다. 하나로 뭉치면 {@code if (TICKET_ISSUED)} 분기가 생기고,
 * 템플릿이 늘 때마다 그 메서드가 커진다. Spring 이 구현을 전부 모아 주므로
 * {@link com.expo.notification.service.NotificationTextRebuilders} 가 코드로 골라 준다.
 */
public interface NotificationTextRebuilder {

    /** 이 재구성기가 맡는 템플릿. {@code notifications.template_code} 와 같아야 한다. */
    String templateCode();

    /**
     * 본문을 만든다.
     *
     * <p><b>부수효과가 있을 수 있다.</b> {@code TICKET_ISSUED} 는 새 접근 토큰을 발급하고 이전 토큰을
     * 폐기한다. 호출자의 트랜잭션 안에서 불려야 하고, 발송이 실패하면 함께 롤백된다.
     *
     * @throws com.expo.common.exception.BusinessException 복원에 필요한 값이 없을 때
     */
    String rebuild(Notification notification);
}
