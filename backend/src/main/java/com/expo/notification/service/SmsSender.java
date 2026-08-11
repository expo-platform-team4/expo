package com.expo.notification.service;

import com.expo.notification.dto.MessageSendResult;

/**
 * SMS 한 건을 보낸다.
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
     * @param to 수신 번호 (숫자만, 예 {@code 01012345678})
     * @param text 본문
     */
    MessageSendResult send(String to, String text);
}
