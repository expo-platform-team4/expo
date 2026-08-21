package com.expo.common.config;

import com.expo.common.exception.ApiAccessDeniedHandler;
import com.expo.common.exception.ApiAuthenticationEntryPoint;
import com.expo.jwt.JwtAuthenticationFilter;
import com.expo.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 전역 설정.
 *
 * <p>이 프로젝트는 세션 로그인(formLogin) 대신 <b>JWT Stateless 인증</b>을 사용한다. HTTP 요청이 들어올 때마다 {@link
 * JwtAuthenticationFilter}가 토큰을 검사해 {@link
 * org.springframework.security.core.context.SecurityContext}를 채우고, 아래 URL 규칙에 따라 접근을 허용·거부한다.
 *
 * <h2>요청 처리 순서 (요약)</h2>
 *
 * <pre>
 * 1. JwtAuthenticationFilter  → Authorization 헤더 JWT 검증, AuthPrincipal 설정
 * 2. authorizeHttpRequests    → URL·역할(hasRole) 기준 접근 제어
 * 3. 컨트롤러                  → @AuthenticationPrincipal 등으로 사용자 정보 사용
 * </pre>
 *
 * <p>{@code hasRole("MEMBER")}는 내부적으로 {@code ROLE_MEMBER} 권한과 비교한다 ({@link
 * com.expo.auth.Role#getAuthority()}).
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /** 매 요청마다 JWT를 읽어 SecurityContext를 설정하는 필터. */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 인증되지 않은 요청(토큰 없음·만료·위조)에 401 을 돌려준다. */
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;

    /** 인증은 됐지만 역할이 모자란 요청에 403 을 돌려준다. */
    private final ApiAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /**
     * Spring Security 필터 체인·인가 규칙을 정의한다.
     *
     * <p>Spring Boot 3.x 에서는 {@code WebSecurityConfigurerAdapter} 대신 {@link SecurityFilterChain} 빈을
     * 등록하는 방식을 쓴다.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API + JWT: 브라우저 폼 제출이 아니므로 CSRF 보호 비활성화
                .csrf(csrf -> csrf.disable())
                // 세션을 만들지 않음. 로그인 상태는 JWT + SecurityContext(요청 스코프)로만 유지
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // /login 페이지 기반 폼 로그인 미사용
                .formLogin(form -> form.disable())
                // Authorization: Basic 헤더 인증 미사용
                .httpBasic(basic -> basic.disable())
                // URL 별 인증·인가 규칙 (위에서부터 먼저 매칭된 규칙 적용)
                //
                // 기본값은 anyRequest().authenticated() 다 — 규칙을 안 적은 경로는 막힌다(이슈 #94).
                // 반대로 두면 새 컨트롤러가 조용히 공개된다. 컴파일도 되고 테스트도 통과하고
                // 기동도 되기 때문에 아무도 모른다. 실제로 POST /api/orders/member 가
                // 인증 없이 호출 가능한 상태였고, principal 이 null 이라 500 이 났다.
                //
                // 공개해야 하는 것은 아래에 하나씩 올린다. 새 경로를 추가하는 사람은
                // "안 적으면 막힌다" 는 안전한 방향으로 실수하게 된다.
                .authorizeHttpRequests(
                        auth ->
                                auth
                                        // ---------- 인증 없이 접근 ----------
                                        // 로그인·회원가입·토큰 갱신·중복확인
                                        .requestMatchers("/api/auth/**")
                                        .permitAll()
                                        // 토큰이 곧 인증인 공개 API (SMS 링크의 QR 확인 등)
                                        .requestMatchers("/api/public/**")
                                        .permitAll()
                                        // 둘러보기 — 비회원도 박람회를 보고 티켓을 고를 수 있어야 한다
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/expos",
                                                "/api/expos/*",
                                                "/api/expos/*/ticket-products/purchasable")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/recruitment-notices",
                                                "/api/recruitment-notices/*")
                                        .permitAll()
                                        // 장소 카탈로그(읽기 전용). availability 는 여기 없다 —
                                        // 부르는 화면이 없어 기본값(인증 필요)에 맡긴다.
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/virtual-venues",
                                                "/api/virtual-venues/*/halls",
                                                "/api/venue-halls/*/zones")
                                        .permitAll()
                                        // 비회원 예매 — 로그인 없이 사는 흐름 자체가 상품이다
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/orders/guest",
                                                "/api/orders/search/guest")
                                        .permitAll()
                                        // 결제 — 게스트 주문도 승인해야 해서 열어 둔다.
                                        // 회원 주문의 소유자 확인은 TicketOrderAccessVerifier 가
                                        // 한다(주문이 MEMBER 면 principal 과 대조).
                                        .requestMatchers(HttpMethod.POST, "/api/payments/**")
                                        .permitAll()
                                        // 파일 내려받기·메타데이터. 프론트 인증이 Authorization
                                        // 헤더라 <img src> 가 토큰을 실을 수 없어서, 공개 이미지를
                                        // 그리려면 열려 있어야 한다. 비공개 파일을 막는 일은
                                        // FileService 가 파일마다 판정한다.
                                        // (업로드·삭제는 규칙을 적지 않아 기본값으로 막힌다)
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/files/*",
                                                "/api/files/*/content")
                                        .permitAll()
                                        // API 문서
                                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                                        .permitAll()
                                        // 헬스체크 — 리버스 프록시·배포 환경이 인증 없이 부른다
                                        .requestMatchers("/actuator/health/**", "/actuator/info")
                                        .permitAll()
                                        // 에러 디스패치. 막으면 예외 응답이 401 로 덮인다
                                        .requestMatchers("/error")
                                        .permitAll()
                                        // OAuth2 는 아직 백엔드에 붙어 있지 않지만 프론트에
                                        // rewrite 가 이미 있다. 붙이는 순간 로그인 자체가
                                        // 막히지 않도록 미리 열어 둔다.
                                        .requestMatchers("/oauth2/**", "/login/oauth2/**")
                                        .permitAll()

                                        // ---------- 역할이 필요한 경로 ----------
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("ADMIN")
                                        // 내부 배치·운영 호출. 정산 대상을 만드는 API 가 여기 있다
                                        .requestMatchers("/api/internal/**")
                                        .hasRole("ADMIN")
                                        // 클라이언트(주최사·참여 기업) API.
                                        // 둘이 같은 역할이라 박람회 소유자 판정은 서비스가 한다
                                        .requestMatchers("/api/client/**")
                                        .hasAnyRole("CLIENT", "ADMIN")
                                        .requestMatchers("/api/member/**")
                                        .hasRole("MEMBER")
                                        // 회원 환불 API — ROLE_MEMBER 필요
                                        .requestMatchers("/api/members/**")
                                        .hasRole("MEMBER")
                                        // 마이페이지(회원·클라이언트 공통) — MEMBER 또는 CLIENT
                                        .requestMatchers("/api/users/**")
                                        .hasAnyRole("MEMBER", "CLIENT")

                                        // ---------- 기본: 로그인 필요 ----------
                                        .anyRequest()
                                        .authenticated())
                // 인증 실패는 401, 권한 부족은 403. 등록하지 않으면 기본값이 둘 다 403 이라
                // 프론트의 401 재발급 인터셉터가 만료된 토큰을 갱신하지 못한다.
                .exceptionHandling(
                        exception ->
                                exception
                                        .authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(accessDeniedHandler))
                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 배치해 토큰을 먼저 처리
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 비밀번호 단방향 해시용 인코더.
     *
     * <p>로그인 시 DB에 저장된 해시와 사용자 입력 비밀번호를 비교할 때 사용한다. JWT 발급 전 회원 인증 로직(추후 구현)에서 주입받아 쓴다.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
