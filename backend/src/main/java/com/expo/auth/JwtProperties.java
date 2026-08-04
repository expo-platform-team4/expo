package com.expo.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 관련 설정값을 {@code application.yml}의 {@code app.jwt} 섹션에서 읽어 오는 설정 클래스.
 *
 * <p>Spring Boot의 {@link ConfigurationProperties}를 사용해 YAML/환경 변수를 타입 안전한 Java
 * 객체로 바인딩한다. {@link com.expo.common.config.SecurityConfig}의 {@code
 * @EnableConfigurationProperties(JwtProperties.class)}로 빈으로 등록되며, {@link JwtTokenProvider}가
 * 토큰 서명·만료 시간 계산에 이 값을 사용한다.
 *
 * <h2>설정 예시 ({@code application.yml})</h2>
 *
 * <pre>{@code
 * app:
 *   jwt:
 *     secret: ${JWT_SECRET}
 *     access-token-expire-minutes: ${JWT_ACCESS_EXPIRE_MINUTES:30}
 * }</pre>
 *
 * <p>kebab-case YAML 키({@code access-token-expire-minutes})는 camelCase 필드({@code
 * accessTokenExpireMinutes})에 자동 매핑된다.
 *
 * <p>{@code @Validated}와 필드 제약({@code @NotBlank}, {@code @Positive})으로 애플리케이션 기동
 * 시 필수 값·유효 범위를 검증한다. {@code secret}이 비어 있거나 만료 시간이 0 이하이면 기동이
 * 실패한다.
 */
//application.yml의 app.jwt 아래 설정을 이 클래스 필드에 자동으로 넣어 줍니다.
@ConfigurationProperties(prefix = "app.jwt")
@Validated
//@Validated: 필드 값 검증을 위한 어노테이션
public class JwtProperties {

  /**
   * JWT 서명(HMAC-SHA)에 쓰는 비밀 키.
   *
   * <p>토큰 생성 시 서명하고, 검증 시 같은 키로 위변조 여부를 확인한다. 키가 다르거나 유출되면
   * 토큰을 신뢰할 수 없으므로 운영 환경에서는 반드시 환경 변수({@code JWT_SECRET})로 주입하고
   * 저장소에 커밋하지 않는다.
   *
   * <p>HS256 알고리즘 기준 최소 32바이트(256비트) 이상을 권장한다. {@link JwtTokenProvider} 생성자에서
   * 이 문자열을 {@code SecretKey}로 변환한다.
   */
  @NotBlank
  private String secret;

  /**
   * Access Token 유효 기간(분).
   *
   * <p>로그인 후 발급되는 Access Token이 몇 분 뒤 만료되는지 결정한다. {@link
   * JwtTokenProvider#createAccessToken}에서 {@code issuedAt + N분}으로 {@code exp} 클레임을
   * 설정한다.
   *
   * <p>기본값은 {@code application.yml}에서 {@code 30}분. 값이 짧을수록 보안은 좋아지지만
   * 재로그인·토큰 갱신 빈도가 늘어난다.
   */
  @Positive
  private int accessTokenExpireMinutes;

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }
//토큰 만료 시간 계산 시 사용
  public int getAccessTokenExpireMinutes() {
    return accessTokenExpireMinutes;
  }
//YAML 설정값 주입
  public void setAccessTokenExpireMinutes(int accessTokenExpireMinutes) {
    this.accessTokenExpireMinutes = accessTokenExpireMinutes;
  }
}
