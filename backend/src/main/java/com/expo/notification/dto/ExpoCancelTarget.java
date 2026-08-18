package com.expo.notification.dto;

/**
 * 박람회 취소 안내를 받을 대상 하나. <b>수신번호 단위</b>다.
 *
 * <h2>왜 주문 단위가 아닌가</h2>
 *
 * 처음에는 주문 단위로 잡았다가 바꿨다. 이유가 둘이다.
 *
 * <p><b>대행사가 번호 단위로 중복을 걸러낸다.</b> 한 사람이 같은 박람회 주문을 두 건 가지고 있으면 같은 번호가
 * 요청에 두 번 들어가고, 대행사는 하나만 남기고 나머지를 실패 처리한다({@code statusCode 1026}). 그러면
 * <b>우리가 잘못 넣은 것이 "발송 실패" 로 기록되고 재시도 대상까지 된다.</b>
 *
 * <p><b>알림의 참조가 박람회다.</b> 취소 안내는 주문에 딸린 사건이 아니라 박람회에 딸린 사건이라
 * {@code reference_type} 이 {@code EXPO} 다. 그러면 알림 1행은 "주문 하나" 가 아니라
 * <b>"한 사람에게 보낸 안내 하나"</b> 가 자연스럽다.
 *
 * <p>그래서 문자 본문에 주문번호를 넣지 않는다. 여러 주문을 가진 사람에게 어느 하나만 적으면 오히려 헷갈린다.
 *
 * @param userId 회원이면 값이 있고 비회원이면 {@code null}
 * @param phoneNumber 수신 번호. 없을 수 있다 — 그래도 목록에는 남는다
 * @param sampleTicketOrderId 이 사람의 주문 중 하나. 추적용이고 본문에는 쓰지 않는다
 */
public record ExpoCancelTarget(Long userId, String phoneNumber, Long sampleTicketOrderId) {

    /** 발송기가 쓰는 형태로 바꾼다. */
    public NotificationRecipient recipient() {
        return new NotificationRecipient(userId, phoneNumber);
    }
}
