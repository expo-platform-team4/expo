package com.expo.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * SMTP 로 실제 이메일을 보낸다. {@code app.mail.provider=smtp} 일 때만 빈으로 만들어진다.
 *
 * <p>{@code spring.mail.*} 자동설정 대신 {@link JavaMailSenderImpl} 을 직접 구성한다 — SMS 발송의
 * {@code SolapiSmsSender}와 같은 이유다. 자동설정은 값이 비어 있어도(플레이스홀더가 안 풀려도) 조용히 통과시킨다.
 * 여기서는 {@link #required} 로 직접 확인해서, 이 빈이 실제로 만들어지는 시점(즉 실제 발송을 하겠다는
 * 뜻)에 값이 없으면 기동을 막는다.
 *
 * <h2>Gmail 계정 비밀번호가 아니라 앱 비밀번호</h2>
 *
 * 2단계 인증을 켠 뒤 https://myaccount.google.com/apppasswords 에서 발급한 16자리 값을 써야 한다.
 * 계정 비밀번호 그대로 넣으면 SMTP 인증이 거부된다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSenderImpl mailSender;
    private final String from;
    private final EmailMasker emailMasker;

    public SmtpEmailSender(EmailProperties properties, EmailMasker emailMasker) {
        this.emailMasker = emailMasker;
        String host = required(properties.getHost(), "MAIL_SMTP_HOST");
        String username = required(properties.getUsername(), "MAIL_SMTP_USERNAME");
        String password = required(properties.getPassword(), "MAIL_SMTP_PASSWORD");
        this.from = resolveFrom(properties.getFrom(), username);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(properties.getPort());
        sender.setUsername(username);
        sender.setPassword(password);
        sender.getJavaMailProperties().put("mail.transport.protocol", "smtp");
        sender.getJavaMailProperties().put("mail.smtp.auth", "true");
        sender.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");
        this.mailSender = sender;
    }

    /**
     * 설정값을 확인한다. {@code ${...}} 검사가 있는 이유는 {@code SolapiSmsSender.required} 와 같다 —
     * {@code @ConfigurationProperties} 바인딩은 치환하지 못한 플레이스홀더를 예외 없이 원문 그대로
     * 넘긴다. 환경변수를 빠뜨리면 문자열 {@code "${MAIL_SMTP_...}"} 를 그대로 들고 기동해 버린다.
     */
    private static String required(String value, String envName) {
        if (value == null || value.isBlank() || value.startsWith("${")) {
            throw new IllegalStateException(envName + " 이(가) 설정되지 않았습니다. backend/.env 를 확인하십시오.");
        }
        return value;
    }

    /** {@code app.mail.smtp.from} 이 비어 있으면 로그인 계정을 발신자로 쓴다. */
    private static String resolveFrom(String from, String username) {
        if (from == null || from.isBlank() || from.startsWith("${")) {
            return username;
        }
        return from;
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("메일 발송 완료 to={}", emailMasker.mask(to));
        } catch (MessagingException | MailException e) {
            // 메일 한 통이 실패했다고 이미 커밋된 토큰 발급을 되돌릴 이유는 없다 — 로그만 남긴다.
            log.warn("메일 발송 실패 to={}", emailMasker.mask(to), e);
        }
    }
}
