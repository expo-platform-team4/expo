package com.expo.checkin.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * QR 토큰 서명 설정 ({@code app.qr}).
 *
 * <p>{@code tokenSecret} 은 <b>JWT 시크릿과 반드시 다른 값</b>이어야 한다. 하나가 유출됐을 때 로그인 토큰과 입장권이 함께 무너지면 안 된다.
 *
 * <p>이 값을 바꾸면 이미 발권된 티켓의 QR 이 전부 달라져 체크인이 실패한다. 교체하려면 재발권이 필요하다. 그래서 QR 원문에 키 버전 접두어를 붙여 둔다 —
 * {@link QrTokenGenerator} 참고.
 */
@ConfigurationProperties(prefix = "app.qr")
@Validated
public class QrTokenProperties {

    /**
     * QR 원문 HMAC 서명 키.
     *
     * <p>세 가지를 검사한다. 특히 세 번째가 중요하다.
     *
     * <ul>
     *   <li>{@code @NotBlank} — 비어 있으면 기동 실패
     *   <li>{@code @Size} — HMAC-SHA256 키는 최소 256비트여야 한다
     *   <li>{@code @Pattern} — <b>환경변수 미치환을 잡는다</b>
     * </ul>
     *
     * <p>{@code @ConfigurationProperties} 바인딩은 {@code @Value} 와 달리 <b>치환하지 못한 플레이스홀더에서 예외를 던지지
     * 않고 원문을 그대로 넘긴다.</b> 그래서 {@code QR_TOKEN_SECRET} 을 설정하지 않으면 이 필드가 문자열 {@code
     * "${QR_TOKEN_SECRET}"} 이 되고, 비어 있지 않으니 {@code @NotBlank} 를 통과해 앱이 멀쩡히 뜬다. 저장소만 보면 누구나 아는 값으로
     * 모든 티켓 QR 이 서명되는 상태다. 실제로 이 방식으로 한 번 통과했다.
     */
    @NotBlank
    @Size(min = 32, message = "QR 서명 키는 최소 32자여야 합니다. openssl rand -base64 48 로 생성하십시오.")
    @Pattern(
            regexp = "^(?!\\$\\{).+$",
            message = "QR_TOKEN_SECRET 환경변수가 설정되지 않았습니다. backend/.env 를 확인하십시오.")
    private String tokenSecret;

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }
}
