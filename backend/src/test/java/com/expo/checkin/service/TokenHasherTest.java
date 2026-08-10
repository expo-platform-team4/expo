package com.expo.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** 토큰 해시가 조회 키로 쓸 수 있을 만큼 안정적인지, 컬럼 폭에 들어가는지 검증한다. */
class TokenHasherTest {

    private final TokenHasher hasher = new TokenHasher();

    /** UNIQUE 인덱스로 조회하므로 같은 입력은 반드시 같은 해시여야 한다. BCrypt 를 쓰지 않은 이유이기도 하다. */
    @Test
    void sameInputAlwaysGivesSameHash() {
        assertThat(hasher.hash("token-abc")).isEqualTo(hasher.hash("token-abc"));
    }

    @Test
    void differentInputGivesDifferentHash() {
        assertThat(hasher.hash("token-abc")).isNotEqualTo(hasher.hash("token-abd"));
    }

    /** qr_token_hash·token_hash 는 VARCHAR(255) 다. 64자면 넉넉히 들어간다. */
    @Test
    void hashIsSixtyFourLowercaseHexChars() {
        assertThat(hasher.hash("token-abc")).hasSize(64).matches("^[0-9a-f]{64}$");
    }

    @Test
    void matchesOnlyTheRightValue() {
        String stored = hasher.hash("token-abc");

        assertThat(hasher.matches("token-abc", stored)).isTrue();
        assertThat(hasher.matches("token-abd", stored)).isFalse();
    }

    /** 만료·미발급 토큰을 조회하면 null 이 올라온다. 예외가 아니라 불일치로 다뤄야 한다. */
    @Test
    void nullIsNeverAMatch() {
        assertThat(hasher.matches(null, hasher.hash("token-abc"))).isFalse();
        assertThat(hasher.matches("token-abc", null)).isFalse();
    }

    @Test
    void rejectsNullWhenHashing() {
        assertThatThrownBy(() -> hasher.hash(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
