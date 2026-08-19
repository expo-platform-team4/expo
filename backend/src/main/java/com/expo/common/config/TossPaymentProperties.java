package com.expo.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 토스페이먼츠 연동 설정 ({@code app.toss}).
 *
 * <p>키 미발급 상태에서도 애플리케이션은 기동돼야 하므로 값이 비어 있어도 검증에 걸리지 않는다. 대신 {@link
 * com.expo.common.config.TossPaymentClient} 가 실제 결제 승인 호출 시점에 비어 있으면 막는다.
 */
@ConfigurationProperties(prefix = "app.toss")
public class TossPaymentProperties {

    private String clientKey;
    private String secretKey;
    private String apiBaseUrl;

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }
}
