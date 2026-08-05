package com.expo.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 관련 설정값을 {@code application.yml}의 {@code app.jwt} 섹션에서 읽어 오는 설정 클래스.
 *
 * <p>Spring Boot의 {@link ConfigurationProperties}를 사용해 YAML/환경 변수를 타입 안전한 Java 객체로 바인딩한다. {@link
 * com.expo.common.config.SecurityConfig}의
 * {@code @EnableConfigurationProperties(JwtProperties.class)}로 빈으로 등록되며, {@link JwtTokenProvider}가
 * 토큰 서명·만료 시간 계산에 이 값을 사용한다.
 */
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {

    @NotBlank private String secret;

    @Positive private int accessTokenExpireMinutes;

    @Positive private int refreshTokenExpireDays = 14;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getAccessTokenExpireMinutes() {
        return accessTokenExpireMinutes;
    }

    public void setAccessTokenExpireMinutes(int accessTokenExpireMinutes) {
        this.accessTokenExpireMinutes = accessTokenExpireMinutes;
    }

    public int getRefreshTokenExpireDays() {
        return refreshTokenExpireDays;
    }

    public void setRefreshTokenExpireDays(int refreshTokenExpireDays) {
        this.refreshTokenExpireDays = refreshTokenExpireDays;
    }
}
