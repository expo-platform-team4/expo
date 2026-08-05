package com.expo.common.config;

import com.expo.auth.JwtAuthenticationFilter;
import com.expo.auth.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * <p>이 프로젝트는 세션 로그인(formLogin) 대신 <b>JWT Stateless 인증</b>을 사용한다. HTTP 요청이 들어올
 * 때마다 {@link JwtAuthenticationFilter}가 토큰을 검사해 {@link
 * org.springframework.security.core.context.SecurityContext}를 채우고, 아래 URL 규칙에 따라 접근을
 * 허용·거부한다.
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

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Spring Security 필터 체인·인가 규칙을 정의한다.
     *
     * <p>Spring Boot 3.x 에서는 {@code WebSecurityConfigurerAdapter} 대신 {@link
     * SecurityFilterChain} 빈을 등록하는 방식을 쓴다.
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
                .authorizeHttpRequests(
                        auth ->
                                auth
                                        // 로그인·회원가입·토큰 갱신 등 — 토큰 없이 접근 가능
                                        .requestMatchers("/api/auth/**")
                                        .permitAll()
                                        // 공개 API
                                        .requestMatchers("/api/public/**")
                                        .permitAll()
                                        // Swagger UI
                                        .requestMatchers("/swagger-ui/**")
                                        .permitAll()
                                        // OpenAPI JSON
                                        .requestMatchers("/v3/api-docs/**")
                                        .permitAll()
                                        // 관리자 전용 — ROLE_ADMIN 필요
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("ADMIN")
                                        // 클라이언트(업체) API — CLIENT 또는 ADMIN
                                        .requestMatchers("/api/client/**")
                                        .hasAnyRole("CLIENT", "ADMIN")
                                        // 일반 회원 API — ROLE_MEMBER 필요
                                        .requestMatchers("/api/member/**")
                                        .hasRole("MEMBER")
                                        // 위에 해당하지 않는 나머지 URL — 인증 없이 허용 (필요 시 authenticated()로 변경)
                                        .anyRequest()
                                        .permitAll())
                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 배치해 토큰을 먼저 처리
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 비밀번호 단방향 해시용 인코더.
     *
     * <p>로그인 시 DB에 저장된 해시와 사용자 입력 비밀번호를 비교할 때 사용한다. JWT 발급 전 회원 인증
     * 로직(추후 구현)에서 주입받아 쓴다.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
