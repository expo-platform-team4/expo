package com.expo.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private static final String TEST_SECRET =
      "dGVzdC1vbmx5LXNlY3JldC1kby1ub3QtdXNlLWluLXByb2R1Y3Rpb24=";

  private JwtTokenProvider jwtTokenProvider;
  private SecretKey signingKey;

  @BeforeEach
  void setUp() {
    JwtProperties jwtProperties = new JwtProperties();
    jwtProperties.setSecret(TEST_SECRET);
    jwtProperties.setAccessTokenExpireMinutes(30);
    jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    signingKey = jwtTokenProvider.getSigningKey();
  }

  @Test
  void createAndParseMemberToken() {
    String token = jwtTokenProvider.createAccessToken(1L, Role.MEMBER);

    assertThat(jwtTokenProvider.validateAccessToken(token)).isTrue();
    assertThat(jwtTokenProvider.getMemberId(token)).isEqualTo(1L);
    assertThat(jwtTokenProvider.getRole(token)).isEqualTo(Role.MEMBER);
    assertThat(jwtTokenProvider.isExpired(token)).isFalse();
  }

  @Test
  void createAndParseClientToken() {
    String token = jwtTokenProvider.createAccessToken(2L, Role.CLIENT);

    assertThat(jwtTokenProvider.validateAccessToken(token)).isTrue();
    assertThat(jwtTokenProvider.getMemberId(token)).isEqualTo(2L);
    assertThat(jwtTokenProvider.getRole(token)).isEqualTo(Role.CLIENT);
  }

  @Test
  void createAndParseAdminToken() {
    String token = jwtTokenProvider.createAccessToken(3L, Role.ADMIN);

    assertThat(jwtTokenProvider.validateAccessToken(token)).isTrue();
    assertThat(jwtTokenProvider.getMemberId(token)).isEqualTo(3L);
    assertThat(jwtTokenProvider.getRole(token)).isEqualTo(Role.ADMIN);
  }

  @Test
  void rejectExpiredToken() {
    Date past = new Date(System.currentTimeMillis() - 10_000L);
    Date expired = new Date(System.currentTimeMillis() - 5_000L);

    String token =
        Jwts.builder()
            .subject("1")
            .claim(JwtTokenProvider.ROLE_CLAIM, Role.MEMBER.name())
            .claim(JwtTokenProvider.TOKEN_TYPE_CLAIM, JwtTokenProvider.ACCESS_TOKEN_TYPE)
            .issuedAt(past)
            .expiration(expired)
            .signWith(signingKey)
            .compact();

    assertThat(jwtTokenProvider.validateAccessToken(token)).isFalse();
    assertThat(jwtTokenProvider.isExpired(token)).isTrue();
  }

  @Test
  void rejectInvalidSignatureToken() {
    JwtProperties otherProperties = new JwtProperties();
    otherProperties.setSecret("another-secret-key-for-invalid-signature-test");
    otherProperties.setAccessTokenExpireMinutes(30);
    JwtTokenProvider otherProvider = new JwtTokenProvider(otherProperties);

    String token = otherProvider.createAccessToken(1L, Role.MEMBER);

    assertThat(jwtTokenProvider.validateAccessToken(token)).isFalse();
  }

  @Test
  void rejectTamperedToken() {
    String token = jwtTokenProvider.createAccessToken(1L, Role.MEMBER);
    String tamperedToken = token.substring(0, token.length() - 1) + "X";

    assertThat(jwtTokenProvider.validateAccessToken(tamperedToken)).isFalse();
  }

  @Test
  void rejectNonAccessTokenType() {
    String token =
        Jwts.builder()
            .subject("1")
            .claim(JwtTokenProvider.ROLE_CLAIM, Role.MEMBER.name())
            .claim(JwtTokenProvider.TOKEN_TYPE_CLAIM, "REFRESH")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 60_000L))
            .signWith(signingKey)
            .compact();

    assertThat(jwtTokenProvider.validateAccessToken(token)).isFalse();
  }
}
