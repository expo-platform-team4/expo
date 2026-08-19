package com.expo.notification.service;

import com.expo.notification.dto.MessageSendResult;
import com.expo.notification.dto.SmsMessage;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 실제로 보내지 않고 로그만 남기는 발송기. <b>로컬 개발 전용이다.</b>
 *
 * <p>{@code app.sms.provider} 를 지정하지 않으면 이것이 선택된다. 발신번호 등록이나 과금 없이 발권 → 알림 파이프라인 전체를 돌려볼 수 있다.
 *
 * <p>본문에 QR 확인 링크가 들어 있고 그 링크의 토큰은 <b>그 자체가 인증 수단</b>이다. 원래 로그에 남기면 안 되는 값이지만, 로컬에서 링크를 열어 보는 것이
 * 이 구현의 존재 이유라 DEBUG 로 남긴다. <b>운영에서 이 구현이 선택되면 안 된다</b> — 기동 시 WARN 으로 경고한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "logging", matchIfMissing = true)
public class LoggingSmsSender implements SmsSender {

    private final PhoneNumberMasker phoneNumberMasker;

    public LoggingSmsSender(PhoneNumberMasker phoneNumberMasker) {
        this.phoneNumberMasker = phoneNumberMasker;
    }

    @PostConstruct
    void warnNotRealSender() {
        log.warn("SMS 발송기가 logging 이다. 실제 문자는 발송되지 않는다. 운영이라면 app.sms.provider=solapi 로 바꿔야 한다.");
    }

    @Override
    public MessageSendResult send(String to, String text) {
        log.info("[SMS 미발송] to={} textLength={}", phoneNumberMasker.mask(to), text.length());
        log.debug("[SMS 미발송] 본문\n{}", text);
        return MessageSendResult.accepted(null, null, null);
    }

    /**
     * 대량 발송도 로그만 남긴다.
     *
     * <p>수신번호를 하나하나 찍지 않는다. 1,000건이면 로그가 1,000줄 늘어나 정작 봐야 할 것이 묻힌다.
     * 본문은 <b>첫 건만</b> DEBUG 로 남긴다 — 대량 발송은 문구가 같으므로 하나만 보면 된다.
     */
    @Override
    public List<MessageSendResult> sendMany(List<SmsMessage> messages) {
        log.info("[SMS 미발송] 대량 {}건", messages.size());
        if (!messages.isEmpty()) {
            log.debug("[SMS 미발송] 대량 본문 (첫 건)\n{}", messages.get(0).text());
        }
        return messages.stream().map(m -> MessageSendResult.accepted(null, null, null)).toList();
    }
}
