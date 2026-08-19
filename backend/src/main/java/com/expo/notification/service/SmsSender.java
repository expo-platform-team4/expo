package com.expo.notification.service;

import com.expo.notification.dto.MessageSendResult;
import com.expo.notification.dto.SmsMessage;
import java.util.List;

/**
 * SMS 를 보낸다.
 *
 * <p>구현이 둘이고 {@code app.sms.provider} 로 고른다.
 *
 * <ul>
 *   <li>{@code logging} (기본) — {@link LoggingSmsSender}. 실제로 보내지 않고 로그만 남긴다
 *   <li>{@code solapi} — {@link SolapiSmsSender}. 실제 발송. 발신번호가 사전 등록되어 있어야 한다
 * </ul>
 *
 * <p><b>예외를 던지지 않는다.</b> 발송 실패는 예외가 아니라 {@link MessageSendResult#success()} 가 {@code false} 인
 * 결과로 돌아온다. 문자가 실패했다고 이미 커밋된 발권을 되돌릴 수는 없고, 실패도 이력으로 남겨야 하기 때문이다.
 */
public interface SmsSender {

    /**
     * 한 통 보낸다.
     *
     * <p>대량 발송({@link #sendMany}) 으로 1건을 보내도 되지만 그러지 않는다. 대행사 응답이 성공 건의 개별
     * {@code messageId} 를 주지 않아, 1건짜리에서도 그룹 ID 만 남게 된다.
     *
     * @param to 수신 번호 (숫자만, 예 {@code 01012345678})
     * @param text 본문
     */
    MessageSendResult send(String to, String text);

    /**
     * 여러 통을 <b>한 요청으로</b> 보낸다.
     *
     * <p>낱건 반복 호출을 쓰지 않는 이유는 둘이다. 대행사가 그러지 말라고 명시했고, 왕복이 한 건에 약 1초라
     * 수십 건만 넘어도 호출부가 그동안 묶인다.
     *
     * <p><b>같은 수신번호를 두 번 넣으면 안 된다.</b> 대행사가 한 요청 안의 중복 수신번호를 자동으로 걸러
     * 하나만 남기고 나머지를 실패 처리한다({@code statusCode 1026}). 그러면 "우리가 잘못 넣은 것" 이
     * "발송 실패" 로 기록된다. 중복 제거는 호출부 책임이다.
     *
     * @param messages 보낼 문자들
     * @return <b>입력과 같은 순서·같은 길이</b>의 결과. {@code i} 번째 결과가 {@code i} 번째 메시지의 것이다
     */
    List<MessageSendResult> sendMany(List<SmsMessage> messages);
}
