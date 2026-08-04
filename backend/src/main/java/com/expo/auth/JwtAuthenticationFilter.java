package com.expo.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * HTTP 요청마다 {@code Authorization} 헤더의 JWT Access Token을 검사하고, 유효하면 Spring Security
 * 인증 상태를 설정하는 서블릿 필터.
 *
 * <p>세션 기반 로그인(formLogin)을 쓰지 않는 <b>Stateless JWT 인증</b>의 핵심 진입점이다. 클라이언트가
 * 보낸 토큰을 해석해 "누가 요청했는지"를 {@link SecurityContextHolder}에 넣어 두면, 이후 컨트롤러의
 * {@code @AuthenticationPrincipal AuthPrincipal}이나 {@code hasRole()} 권한 검사가 동작한다.
 *
 * <p>{@link com.expo.common.config.SecurityConfig}에서 {@code
 * UsernamePasswordAuthenticationFilter} 앞에 등록된다. 필터 순서상 인증 필터보다 먼저 실행되어 JWT를
 * SecurityContext로 변환한다.
 *
 * <h2>요청 처리 흐름</h2>
 *
 * <pre>
 * 1. resolveToken()     → Authorization: Bearer &lt;token&gt; 에서 토큰 문자열만 추출
 * 2. validateAccessToken → 서명·만료·토큰 타입(ACCESS) 검증
 * 3. getMemberId / getRole → 클레임에서 회원 ID·역할 파싱
 * 4. AuthPrincipal 생성  → Spring Security가 다룰 수 있는 사용자 객체로 변환
 * 5. SecurityContext 설정 → 이 HTTP 요청 동안만 "로그인됨" 상태 유지
 * 6. filterChain.doFilter → 다음 필터·컨트롤러로 요청 전달
 * </pre>
 *
 * <p>토큰이 없거나 유효하지 않으면 SecurityContext를 건드리지 않고 그대로 통과한다. 이 경우 인증이
 * 필요한 URL({@code /api/member/**} 등)은 Spring Security가 401/403으로 차단한다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  /** HTTP 헤더 이름. 클라이언트는 {@code Authorization: Bearer <JWT>} 형태로 전송한다. */
  private static final String AUTHORIZATION_HEADER = "Authorization";

  /** Bearer 스킴 접두사. 이 뒤의 문자열만 실제 JWT 토큰이다. */
  private static final String BEARER_PREFIX = "Bearer ";

  /** 토큰 생성·검증·클레임 파싱을 담당하는 컴포넌트. */
  private final JwtTokenProvider jwtTokenProvider;

  public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  /**
   * 요청당 한 번 실행되는 필터 본문.
   *
   * <p>{@link OncePerRequestFilter}를 상속해 동일 요청에서 중복 실행되지 않도록 보장한다.
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // 1) 헤더에서 JWT 문자열 추출 (없으면 null)
    String token = resolveToken(request);

    // 2) 토큰이 있고 Access Token으로 유효할 때만 인증 상태를 설정한다.
    if (token != null && jwtTokenProvider.validateAccessToken(token)) {
      // 3) JWT payload 에서 사용자 식별 정보 꺼내기
      Long memberId = jwtTokenProvider.getMemberId(token);
      Role role = jwtTokenProvider.getRole(token);

      // 4) Spring Security principal 객체 생성
      AuthPrincipal principal = new AuthPrincipal(memberId, role);

      // 5) Authentication 객체 조립
      //    - principal: 누구인지 (AuthPrincipal)
      //    - credentials: null (JWT 방식이라 비밀번호 없음)
      //    - authorities: ROLE_MEMBER 등 권한 목록
      //객체 만들기
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

      // 6) 현재 스레드(=현재 HTTP 요청)의 SecurityContext에 인증 정보 저장
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    // 토큰이 없거나 무효하면 위 if 블록을 건너뛴다 → SecurityContext 비인증 상태 유지

    // 7) 반드시 다음 필터로 넘긴다. 여기서 응답을 끝내지 않는다.
    //이걸 호출하지 않으면 요청이 여기서 멈춥니다. 컨트롤러까지 가지 않고 응답도 안 나갑니다.
    filterChain.doFilter(request, response);
  }

  /**
   * {@code Authorization} 헤더에서 Bearer 토큰만 분리해 반환한다.
   *
   * <p>예시:
   *
   * <ul>
   *   <li>입력: {@code "Bearer eyJhbGciOiJIUzI1NiJ9..."}
   *   <li>출력: {@code "eyJhbGciOiJIUzI1NiJ9..."}
   * </ul>
   *
   * <p>헤더가 없거나, 비어 있거나, {@code Bearer } 로 시작하지 않으면 {@code null}을 반환한다.
   *
   * @param request 현재 HTTP 요청
   * @return JWT 문자열, 없으면 {@code null}
   */
  //HTTP 요청 헤더에서 Bearer 접두사를 제거하고 JWT 문자열만 꺼내는 메서드입니다.
  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }
    return null;
  }
}
