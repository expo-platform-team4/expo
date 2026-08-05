package com.expo.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT Access Token의 생성·검증·클레임 파싱을 담당하는 핵심 컴포넌트.
 *
 * <p>{@link JwtProperties}의 비밀 키·만료 시간 설정을 사용해 HMAC-SHA 서명 토큰을 만들고, {@link
 * JwtAuthenticationFilter} 및 로그인 API에서 이 클래스를 호출한다.
 *
 * <h2>역할 분담</h2>
 *
 * <ul>
 *   <li>로그인 성공 시 → {@link #createAccessToken(Long, Role)} 으로 토큰 발급
 *   <li>API 요청 시 → {@link #validateAccessToken(String)} 으로 유효성 검사
 *   <li>검증 후 → {@link #getMemberId(String)}, {@link #getRole(String)} 으로 사용자 정보 추출
 * </ul>
 *
 * <h2>토큰 payload 구조</h2>
 *
 * <pre>
 * sub        : 회원 ID (문자열, 예: "42")
 * role       : MEMBER | CLIENT | ADMIN
 * tokenType  : "ACCESS" (Refresh Token 과 구분)
 * iat        : 발급 시각
 * exp        : 만료 시각 (iat + accessTokenExpireMinutes)
 * </pre>
 */
@Component
public class JwtTokenProvider {

    /** JWT 커스텀 클레임 키. 회원 역할({@link Role#name()})을 저장한다. */
    static final String ROLE_CLAIM = "role";

    /** JWT 커스텀 클레임 키. Access / Refresh 토큰 종류를 구분한다. */
    static final String TOKEN_TYPE_CLAIM = "tokenType";

    /** Access Token 일 때 {@link #TOKEN_TYPE_CLAIM} 값. Refresh Token 은 별도 타입으로 구분 예정. */
    static final String ACCESS_TOKEN_TYPE = "ACCESS";

    /** 만료 시간 등 JWT 설정. */
    private final JwtProperties jwtProperties;

    /**
     * 토큰 서명·검증에 쓰는 HMAC 키.:JWT가 위조되지 않았다는 것을 증명하기 위해, 서버만 알고 있는 비밀 문자열로 토큰에 “도장”을 찍을 때 쓰는 키입니다.
     *
     * <p>생성자에서 {@link JwtProperties#getSecret()}을 한 번 변환해 캐시한다. 앱 기동 후 키가 바뀌지
     * 않는다.
     */
    private final SecretKey signingKey;

    /**
     * {@link JwtProperties}의 secret 로 서명 키를 초기화한다.
     *
     * @param jwtProperties {@code app.jwt} 설정 (secret, accessTokenExpireMinutes)
     */
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // JWT_SECRET 문자열을 HMAC 서명에 쓸 수 있도록 바이트 배열로 바꾸는 코드입니다.
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        // 바이트 배열을 JWT 서명·검증에 쓸 HMAC 키 객체(SecretKey)로 만드는 줄
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token 문자열을 발급한다.
     *
     * <p>로그인 API에서 회원 인증이 끝난 뒤 호출한다. 반환된 JWT는 클라이언트가 이후 요청의 {@code
     * Authorization: Bearer} 헤더에 실어 보낸다.
     *
     * @param memberId 토큰 subject 에 넣을 회원 PK
     * @param role 토큰 role 클레임에 넣을 역할
     * @return 서명된 JWT compact 문자열
     */
    public String createAccessToken(Long memberId, Role role) {
        // 현재 시간을 나타내는 Date 객체를 만듭니다.
        Date now = new Date();

        // 현재 시간에서 토큰 만료 시간을 더한 시간을 나타내는 Date 객체를 만듭니다.
        Date expiry =
                new Date(now.getTime() + jwtProperties.getAccessTokenExpireMinutes() * 60L * 1000L);

        // Jwts.builder() : JWT 빌더 객체를 생성합니다.
        return Jwts.builder()
                .subject(memberId.toString()) // 토큰 subject 에 넣을 회원 PK
                .claim(ROLE_CLAIM, role.name()) // 토큰 role 클레임에 넣을 역할
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE) // 토큰 tokenType 클레임에 넣을 토큰 타입
                .issuedAt(now) // 토큰 발급 시간
                .expiration(expiry) // 토큰 만료 시간
                .signWith(signingKey) // 토큰 서명에 쓸 HMAC 키
                .compact();
    }

    /**
     * Access Token 이 유효한지 검사한다.
     *
     * <p>다음을 모두 만족해야 {@code true}:
     *
     * <ul>
     *   <li>서명이 올바르고 형식이 유효함
     *   <li>{@code tokenType} 클레임이 {@link #ACCESS_TOKEN_TYPE}
     *   <li>만료 시각(exp)이 현재보다 이후
     * </ul>
     *
     * <p>위 조건 중 하나라도 실패하거나 파싱 예외가 나면 {@code false}를 반환한다 (예외를 밖으로 던지지
     * 않음).
     *
     * @param token Bearer 접두사를 제거한 순수 JWT 문자열
     * @return 유효한 Access Token 이면 {@code true}
     */
    public boolean validateAccessToken(String token) {
        try {
            // JWT를 파싱하고 서명이 맞는지 확인합니다.
            Claims claims = parseClaims(token);
            if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
                return false;
            }
            return !isExpired(claims); // 토큰 만료 시간이 현재 시간보다 이전이면 false를 반환합니다.
        } catch (JwtException | IllegalArgumentException e) {
            return false; // 파싱 예외가 나면 false를 반환합니다.
        }
    }

    /**
     * 토큰 subject 에서 회원 ID를 꺼낸다.
     *
     * <p>유효하지 않은 토큰이면 {@link JwtException} 등이 발생할 수 있다. 호출 전 {@link
     * #validateAccessToken(String)} 으로 검증하는 것이 안전하다.
     *
     * @param token JWT 문자열
     * @return 회원 PK
     */
    public Long getMemberId(String token) {
        return Long.parseLong(
                parseClaims(token).getSubject()); // 토큰 subject 에서 회원 PK를 꺼내서 Long 타입으로 반환합니다.
    }

    /**
     * 토큰 role 클레임에서 {@link Role} enum 을 꺼낸다.
     *
     * @param token JWT 문자열
     * @return 회원 역할
     */
    public Role getRole(String token) {
        return Role.valueOf(parseClaims(token).get(ROLE_CLAIM, String.class));
    }

    /**
     * 토큰이 만료되었는지 확인한다.
     *
     * <p>파싱에 실패하면 만료된 것으로 간주해 {@code true}를 반환한다.
     *
     * @param token JWT 문자열
     * @return 만료되었거나 파싱 불가면 {@code true}
     */
    public boolean isExpired(String token) {
        try {
            return isExpired(parseClaims(token));
            // JWT 파싱·검증 중 오류가 나면 “만료된 토큰”으로 간주하는 예외 처리
            // IllegalArgumentException 메서드에 넘긴 인자가 잘못됐을 때 발생하는 예외
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    /** 테스트에서 동일 키로 토큰을 조작·검증할 때 사용. 프로덕션 비즈니스 코드에서는 직접 호출하지 않는다. */
    SecretKey getSigningKey() {
        return signingKey;
    }

    /**
     * JWT 를 파싱하고 서명을 검증한 뒤 payload({@link Claims})를 반환한다.
     *
     * <p>서명 불일치·형식 오류·만료 등은 {@link JwtException}을 던진다.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    /** {@code exp} 클레임이 현재 시각보다 이전이면 만료로 본다. */
    private boolean isExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}
