package com.expo.auth.service;

/**
 * 이메일을 보낸다.
 *
 * <p>구현이 둘이고 {@code app.mail.provider} 로 고른다.
 *
 * <ul>
 *   <li>{@code logging} (기본) — {@link LoggingEmailSender}. 실제로 보내지 않고 로그만 남긴다
 *   <li>{@code smtp} — {@link SmtpEmailSender}. Gmail SMTP 로 실제 발송한다
 * </ul>
 *
 * <p>SMS 발송({@code com.expo.notification.service.SmsSender})과 달리 이력 테이블({@code
 * message_histories})에 남기지 않는다.
 * 지금은 비밀번호 재설정 한 용도로만 쓰는 단순 발송기라서다 — 카카오·SMS 처럼 여러 채널을 순서대로
 * 시도하거나 재시도할 필요가 없다. 발송 실패는 예외를 던지지 않고 로그로만 남긴다: 메일 한 통이 안 갔다고
 * 이미 발급된 재설정 토큰 자체를 무효로 되돌릴 이유는 없다.
 */
public interface EmailSender {

    /**
     * 한 통 보낸다.
     *
     * @param to 수신 이메일 주소
     * @param subject 제목
     * @param body 본문 (평문)
     */
    void send(String to, String subject, String body);
}
