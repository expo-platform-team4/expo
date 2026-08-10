package com.expo.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * 링크 접근 토큰이 추측 불가능하고 URL 에 그대로 실을 수 있는지 검증한다.
 *
 * <p>이 토큰은 그 자체가 인증 수단이다. 값이 겹치거나 예측되면 남의 티켓 QR 이 열린다.
 */
class AccessTokenGeneratorTest {

    private static final int SAMPLE_COUNT = 1_000;

    private final AccessTokenGenerator generator = new AccessTokenGenerator();

    /** {@link QrTokenGenerator} 와 정반대다. 저쪽은 매번 같아야 하고 이쪽은 매번 달라야 한다. */
    @Test
    void neverRepeats() {
        Set<String> tokens = new HashSet<>();
        IntStream.range(0, SAMPLE_COUNT).forEach(i -> tokens.add(generator.generate()));

        assertThat(tokens).hasSize(SAMPLE_COUNT);
    }

    /** SMS 문자열에 그대로 들어가므로 퍼센트 인코딩이 필요한 문자가 있으면 안 된다. */
    @Test
    void isUrlSafe() {
        assertThat(generator.generate()).matches("^[A-Za-z0-9_-]+$");
    }

    /** 256비트를 base64url 패딩 없이 인코딩하면 43자다. token_hash 이전에 URL 길이도 예측 가능해야 한다. */
    @Test
    void hasExpectedLength() {
        assertThat(generator.generate()).hasSize(43);
    }
}
