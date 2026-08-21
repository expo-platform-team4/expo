package com.expo.auth.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 이메일 SMTP 연동 설정 ({@code app.mail.smtp}).
 *
 * <p>{@code @Validated}/{@code @NotBlank} 를 붙이지 않는다. 이 빈은 발송기를 {@code logging} 으로 두고
 * 개발할 때도 항상 등록되므로, 검증을 걸면 SMTP 계정이 없는 팀원의 앱이 기동하지 않는다.
 *
 * <p>대신 {@link SmtpEmailSender} 생성자가 검증한다 ({@link SolapiProperties}와 같은 패턴) — 그 빈은
 * {@code app.mail.provider=smtp} 일 때만 만들어지므로 실제로 값이 필요한 시점에만 확인하게 된다.
 */
@ConfigurationProperties(prefix = "app.mail.smtp")
public class EmailProperties {

    /** SMTP 서버 주소. Gmail 은 {@code smtp.gmail.com}. */
    private String host;

    /** SMTP 포트. Gmail은 STARTTLS 기준 587. */
    private int port = 587;

    /** SMTP 로그인 계정(이메일 주소). */
    private String username;

    /**
     * SMTP 비밀번호. Gmail 은 계정 비밀번호가 아니라 앱 비밀번호(App Password)를 써야 한다 —
     * 2단계 인증을 켠 뒤 https://myaccount.google.com/apppasswords 에서 발급한다.
     */
    private String password;

    /** 발신자로 표시할 주소. 비어 있으면 {@link #username} 을 그대로 쓴다. */
    private String from;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
