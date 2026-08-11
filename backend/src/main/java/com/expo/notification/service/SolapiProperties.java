package com.expo.notification.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Solapi 연동 설정 ({@code app.solapi}).
 *
 * <p>여기에는 {@code @Validated}/{@code @NotBlank} 를 붙이지 않는다. 이 빈은 발송기를 {@code logging} 으로 두고
 * 개발할 때도 항상 등록되므로, 검증을 걸면 Solapi 키가 없는 팀원의 앱이 기동하지 않는다.
 *
 * <p>대신 {@link SolapiSmsSender} 생성자가 검증한다 — 그 빈은 {@code app.sms.provider=solapi} 일 때만 만들어지므로
 * 실제로 값이 필요한 시점에만 확인하게 된다.
 */
@ConfigurationProperties(prefix = "app.solapi")
public class SolapiProperties {

    /** API Key. Authorization 헤더에 그대로 들어간다. */
    private String apiKey;

    /** API Secret. 서명 계산에만 쓰고 절대 밖으로 내보내지 않는다. */
    private String apiSecret;

    /** 발신번호. Solapi 콘솔에 사전 등록·인증된 번호만 쓸 수 있다. */
    private String senderNumber;

    private String apiBaseUrl = "https://api.solapi.com";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getSenderNumber() {
        return senderNumber;
    }

    public void setSenderNumber(String senderNumber) {
        this.senderNumber = senderNumber;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }
}
