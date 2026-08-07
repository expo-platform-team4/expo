package com.expo.checkin.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * 토큰 원문을 DB 에 저장할 해시로 바꾼다.
 *
 * <p>QR 원문({@code issued_tickets.qr_token_hash})과 링크 접근 토큰({@code ticket_access_tokens.token_hash})이
 * 같은 방식을 쓴다. 둘 다 원문은 저장하지 않는다.
 *
 * <p>비밀번호가 아니라 이미 충분한 엔트로피를 가진 랜덤·서명 값이라 BCrypt 가 아니라 SHA-256 을 쓴다. BCrypt 는 salt 때문에 같은 입력이 매번 다른
 * 결과를 내서 UNIQUE 인덱스로 조회할 수 없다.
 */
@Component
public class TokenHasher {

    private static final String ALGORITHM = "SHA-256";

    /** 원문을 소문자 16진수 64자 해시로 바꾼다. */
    public String hash(String rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException("해시할 원문이 없습니다.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            return HexFormat.of()
                    .formatHex(digest.digest(rawValue.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 은 JDK 표준이라 실제로는 도달하지 않는다.
            throw new IllegalStateException("SHA-256 을 사용할 수 없습니다.", e);
        }
    }

    /**
     * 원문이 저장된 해시와 일치하는지 확인한다.
     *
     * <p>길이가 같을 때 조기 반환하지 않도록 {@link MessageDigest#isEqual} 로 비교한다. 바이트별로 빠져나오면 비교에 걸린 시간으로 해시를 한
     * 글자씩 맞춰 볼 수 있다.
     */
    public boolean matches(String rawValue, String storedHash) {
        if (rawValue == null || storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(rawValue).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}
