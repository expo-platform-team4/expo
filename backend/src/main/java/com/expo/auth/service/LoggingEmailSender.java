package com.expo.auth.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 실제로 보내지 않고 로그만 남기는 발송기. <b>로컬 개발 전용이다.</b>
 *
 * <p>{@code app.mail.provider} 를 지정하지 않으면 이것이 선택된다. SMTP 계정 없이 비밀번호 재설정
 * 흐름 전체를 돌려볼 수 있다.
 *
 * <p>본문에 재설정 토큰이 들어 있고 그 토큰은 <b>그 자체가 인증 수단</b>이다. 원래 로그에 남기면 안
 * 되는 값이지만, 로컬에서 토큰을 손에 넣는 것이 이 구현의 존재 이유라 DEBUG 로 남긴다. <b>운영에서 이
 * 구현이 선택되면 안 된다</b> — 기동 시 WARN 으로 경고한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "logging", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private final EmailMasker emailMasker;

    public LoggingEmailSender(EmailMasker emailMasker) {
        this.emailMasker = emailMasker;
    }

    @PostConstruct
    void warnNotRealSender() {
        log.warn("이메일 발송기가 logging 이다. 실제 메일은 발송되지 않는다. 운영이라면 app.mail.provider=smtp 로 바꿔야 한다.");
    }

    @Override
    public void send(String to, String subject, String body) {
        log.info("[메일 미발송] to={} subject={}", emailMasker.mask(to), subject);
        log.debug("[메일 미발송] 본문\n{}", body);
    }
}
