package com.expo.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * QR 원문 생성이 결정적인지 검증한다.
 *
 * <p>설계 전체가 "원문을 저장하지 않고 필요할 때마다 다시 만든다" 에 걸려 있다. 재생성이 한 번이라도 다른 값을 내면 마이페이지의 QR 이 DB 의 {@code
 * qr_token_hash} 와 어긋나 현장에서 입장이 거부된다. 눈으로 확인할 수 없는 성질이라 테스트로 못박는다.
 */
class QrTokenGeneratorTest {

    private static final String SECRET = "test-qr-secret-value-for-unit-test-only";

    private final QrTokenGenerator generator = generatorWith(SECRET);
    private final TokenHasher tokenHasher = new TokenHasher();

    private static QrTokenGenerator generatorWith(String secret) {
        QrTokenProperties properties = new QrTokenProperties();
        properties.setTokenSecret(secret);
        return new QrTokenGenerator(properties);
    }

    @Test
    void samePayloadEveryTime() {
        String first = generator.generatePayload("EXPO-20260807-000001");
        String second = generator.generatePayload("EXPO-20260807-000001");

        assertThat(first).isEqualTo(second);
    }

    @Test
    void differentTicketsGetDifferentPayloads() {
        String one = generator.generatePayload("EXPO-20260807-000001");
        String other = generator.generatePayload("EXPO-20260807-000002");

        assertThat(one).isNotEqualTo(other);
    }

    /**
     * 코드가 한 자리만 달라도 결과가 통째로 바뀌어야 한다.
     *
     * <p>티켓 코드는 {@code EXPO-날짜-일련번호} 라 순차적이고 예측 가능하다. 그래도 QR 이 서로 겹치거나 이웃 값에서 유추되지 않는다는 것을 못박는다.
     */
    @Test
    void oneCharacterDifferenceChangesEverything() {
        String base = generator.generatePayload("EXPO-20260807-000001");

        assertThat(generator.generatePayload("EXPO-20260807-000002")).isNotEqualTo(base);
        assertThat(generator.generatePayload("EXPO-20260808-000001")).isNotEqualTo(base);
    }

    @Test
    void differentSecretProducesDifferentPayload() {
        String mine = generator.generatePayload("EXPO-20260807-000001");
        String forged = generatorWith("another-secret").generatePayload("EXPO-20260807-000001");

        assertThat(mine).isNotEqualTo(forged);
    }

    /** 키 로테이션 대비 접두어. 나중에 구·신 키를 함께 검증할 때 이걸로 구분한다. */
    @Test
    void payloadCarriesKeyVersionPrefix() {
        assertThat(generator.generatePayload("EXPO-20260807-000001")).startsWith("v1.");
    }

    /** QR 은 URL 과 이미지에 실린다. 인코딩이 필요한 문자가 들어가면 안 된다. */
    @Test
    void payloadIsUrlSafe() {
        String payload = generator.generatePayload("EXPO-20260807-000001");

        assertThat(payload).matches("^v1\\.[A-Za-z0-9_-]+$");
    }

    /** 저장값은 원문이 아니라 해시다. 원문이 그대로 들어가면 DB 유출만으로 QR 이 복제된다. */
    @Test
    void storedHashIsNotThePayload() {
        String payload = generator.generatePayload("EXPO-20260807-000001");
        String stored = tokenHasher.hash(payload);

        assertThat(stored).isNotEqualTo(payload);
        assertThat(tokenHasher.matches(payload, stored)).isTrue();
    }

    @Test
    void rejectsMissingTicketCode() {
        assertThatThrownBy(() -> generator.generatePayload("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
