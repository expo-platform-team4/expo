package com.expo.checkin.service;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * SMS 로 보내는 QR 확인 링크의 접근 토큰 원문을 만든다.
 *
 * <p>{@link QrTokenGenerator} 와 달리 <b>결정적이지 않다.</b> 이 토큰은 그 자체가 인증 수단이라 폐기하고 새로 발급할 수 있어야 한다. 같은
 * 주문에서 매번 다른 값이 나와야 옛 링크를 무효화할 수 있다.
 *
 * <p>원문은 URL 에 실려 나가고 DB 에는 {@link TokenHasher} 해시만 남는다.
 */
@Component
public class AccessTokenGenerator {

    /** 256비트. URL 에 실을 값이라 base64url 로 43자가 된다. */
    private static final int TOKEN_BYTES = 32;

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom secureRandom = new SecureRandom();

    /** URL 에 그대로 넣을 수 있는 접근 토큰 원문을 만든다. */
    public String generate() {
        byte[] buffer = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(buffer);
        return ENCODER.encodeToString(buffer);
    }
}
