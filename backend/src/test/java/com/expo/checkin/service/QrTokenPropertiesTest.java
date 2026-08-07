package com.expo.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * QR 서명 키 설정 검증을 못박는다.
 *
 * <p>핵심은 {@link #rejectsUnresolvedPlaceholder()} 다. {@code @ConfigurationProperties} 바인딩은 {@code
 * @Value} 와 달리 <b>치환하지 못한 플레이스홀더에서 예외를 던지지 않고 원문을 그대로 넘긴다.</b> 그래서 환경변수를 빠뜨리면 시크릿이 문자열 {@code
 * "${QR_TOKEN_SECRET}"} 이 된 채로 앱이 정상 기동한다 — 저장소만 보면 누구나 아는 값으로 모든 티켓 QR 이 서명된다. 실제로 이 방식으로 한 번
 * 통과했고, 그래서 테스트로 남긴다.
 */
class QrTokenPropertiesTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static QrTokenProperties propertiesWith(String secret) {
        QrTokenProperties properties = new QrTokenProperties();
        properties.setTokenSecret(secret);
        return properties;
    }

    @Test
    void acceptsProperSecret() {
        assertThat(validator.validate(propertiesWith("a".repeat(48)))).isEmpty();
    }

    /** 환경변수를 설정하지 않았을 때 실제로 바인딩되는 값이다. 반드시 기동을 막아야 한다. */
    @Test
    void rejectsUnresolvedPlaceholder() {
        assertThat(validator.validate(propertiesWith("${QR_TOKEN_SECRET}"))).isNotEmpty();
    }

    /** 이름이 긴 환경변수라 32자를 넘겨도 플레이스홀더는 여전히 걸러져야 한다. */
    @Test
    void rejectsLongUnresolvedPlaceholder() {
        assertThat(validator.validate(propertiesWith("${VERY_LONG_QR_TOKEN_SECRET_VARIABLE_NAME}")))
                .isNotEmpty();
    }

    /** HMAC-SHA256 키는 최소 256비트여야 한다. */
    @Test
    void rejectsShortSecret() {
        assertThat(validator.validate(propertiesWith("short"))).isNotEmpty();
    }

    @Test
    void rejectsBlankSecret() {
        assertThat(validator.validate(propertiesWith("   "))).isNotEmpty();
    }
}
