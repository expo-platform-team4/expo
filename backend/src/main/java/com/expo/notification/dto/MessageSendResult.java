package com.expo.notification.dto;

/**
 * 발송 시도 한 건의 결과. {@code message_histories} 한 행이 된다.
 *
 * <p>대행사 응답을 필드로 쪼개지 않고 <b>원문 그대로</b> 들고 다닌다. 응답 스키마가 바뀌어도 우리 코드가 깨지지 않고, {@code
 * message_histories.response_payload} 가 JSONB 라 그대로 넣을 수 있다.
 *
 * <p>이름이 {@code Sms} 가 아니라 {@code Message} 인 이유는 {@code message_histories} 가 채널 중립이기 때문이다.
 * 나중에 알림톡·이메일이 붙어도 같은 타입을 쓴다.
 *
 * @param success 대행사가 접수했는지 (HTTP 2xx)
 * @param providerMessageId 대행사 메시지 ID. 응답에서 못 찾으면 {@code null}
 * @param requestPayload 보낸 요청 원문(JSON). <b>수신번호가 들어 있으므로 로그에 남기지 않는다</b>
 * @param responsePayload 받은 응답 원문(JSON). 실패 응답도 그대로 담는다
 * @param errorCode 실패 시 분류용 코드. 성공이면 {@code null}
 */
public record MessageSendResult(
        boolean success,
        String providerMessageId,
        String requestPayload,
        String responsePayload,
        String errorCode) {

    public static MessageSendResult accepted(
            String providerMessageId, String requestPayload, String responsePayload) {
        return new MessageSendResult(
                true, providerMessageId, requestPayload, responsePayload, null);
    }

    public static MessageSendResult failed(
            String errorCode, String requestPayload, String responsePayload) {
        return new MessageSendResult(false, null, requestPayload, responsePayload, errorCode);
    }
}
