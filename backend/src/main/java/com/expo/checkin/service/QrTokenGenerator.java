package com.expo.checkin.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * QR 원문을 만든다.
 *
 * <p><b>결정적(deterministic)이다.</b> 같은 티켓이면 언제 몇 번을 물어도 같은 값이 나온다. 그래서 원문을 저장하지 않고도 마이페이지든 SMS 링크든
 * 어디서나 같은 QR 을 다시 보여줄 수 있다. DB 에는 {@link TokenHasher} 로 만든 해시만 둔다.
 *
 * <pre>
 * 원문   = "v1." + base64url(HMAC-SHA256(시크릿, 티켓코드))
 * 저장값 = SHA-256(원문)   → issued_tickets.qr_token_hash
 * 검증   = 스캔한 원문을 해시해 qr_token_hash 와 대조 (UNIQUE 라 조회 키로도 쓴다)
 * </pre>
 *
 * <p><b>서명 대상은 티켓 코드 하나다.</b> HMAC 의 안전성은 비밀키에서 나오지 메시지의 예측 불가능성에서 나오지 않는다. 코드가 {@code
 * EXPO-20260807-000123} 처럼 순차적이어도 키가 없으면 서명을 만들 수 없다. {@code ticket_code} 는 UNIQUE 라 티켓을 1:1 로 지목한다.
 *
 * <p>티켓 {@code id} 를 함께 서명하려다 뺐다. 안전성에 보태는 것이 없는데, {@code id} 는 INSERT 뒤에야 정해져서 NOT NULL·UNIQUE 인
 * {@code qr_token_hash} 에 임시값을 넣었다가 덮어쓰는 2단계 쓰기를 강요한다.
 *
 * <p>접두어 {@code v1.} 은 키 로테이션 대비다. 시크릿을 바꾸면 기존 QR 이 전부 깨지므로, 나중에 구·신 키를 함께 검증해야 할 때 이 접두어로 구분한다.
 */
@Component
public class QrTokenGenerator {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String KEY_VERSION = "v1";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final String tokenSecret;

    public QrTokenGenerator(QrTokenProperties properties) {
        this.tokenSecret = properties.getTokenSecret();
    }

    /**
     * 발권 티켓 하나의 QR 원문을 만든다. 같은 코드면 언제나 같은 값이 나온다.
     *
     * @param ticketCode {@code issued_tickets.ticket_code}
     */
    public String generatePayload(String ticketCode) {
        if (ticketCode == null || ticketCode.isBlank()) {
            throw new IllegalArgumentException("티켓 코드가 없습니다.");
        }
        return KEY_VERSION + "." + ENCODER.encodeToString(sign(ticketCode));
    }

    private byte[] sign(String message) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("QR 원문 서명에 실패했습니다.", e);
        }
    }
}
