package com.expo.auth;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * JWT 인증이 성공했을 때 {@link org.springframework.security.core.context.SecurityContext}에 저장되는 "인증된 사용자"
 * 정보를 담는 객체.
 *
 * <p>Spring Security는 인증 주체를 {@link UserDetails} 형태로 다루므로, 이 클래스가 해당 인터페이스를 구현한다. {@link
 * JwtAuthenticationFilter}에서 Access Token을 검증한 뒤 토큰에 담긴 memberId·role로 인스턴스를 만들고, {@link
 * org.springframework.security.authentication.UsernamePasswordAuthenticationToken}의 principal로
 * 등록한다.
 *
 * <p>컨트롤러·서비스에서는 아래처럼 현재 로그인 사용자를 꺼낼 수 있다.
 *
 * <pre>{@code
 * @AuthenticationPrincipal AuthPrincipal principal
 * }</pre>
 *
 * <p>비밀번호 기반 로그인(UserDetailsService)이 아닌 JWT Stateless 인증이므로, password·계정 상태 필드는 사용하지 않고 고정값(null
 * 또는 true)을 반환한다.
 */
public class AuthPrincipal implements UserDetails {

  /** DB 회원 테이블의 PK. JWT subject 및 Security "username"으로 사용된다. */
  private final Long memberId;

  /** 회원 역할(MEMBER, CLIENT, ADMIN). URL·메서드 단위 권한 검사에 쓰인다. */
  private final Role role;

  /**
   * Spring Security 권한 목록. 생성 시 {@link Role#getAuthority()}로 한 번만 변환해 캐시한다.
   *
   * <p>예: Role.MEMBER → "ROLE_MEMBER"
   */
  private final List<SimpleGrantedAuthority> authorities;

  /**
   * JWT에서 추출한 식별자·역할로 인증 주체를 만든다.
   *
   * @param memberId 토큰에 포함된 회원 ID
   * @param role 토큰에 포함된 역할
   */
  public AuthPrincipal(Long memberId, Role role) {
    this.memberId = memberId;
    this.role = role;
    // hasRole("MEMBER") 등 @PreAuthorize / requestMatchers.hasRole()이 이 문자열을 비교한다.
    //role.getAuthority()Role enum을 Spring Security 형식 문자열로 바꿉니다.
    //new SimpleGrantedAuthority(...)Spring Security가 쓰는 권한 객체입니다.
    this.authorities = List.of(new SimpleGrantedAuthority(role.getAuthority()));
  }

  /** 애플리케이션 비즈니스 로직에서 쓰는 회원 ID. getUsername()과 동일한 값이지만 Long 타입이다. */
  public Long getMemberId() {
    return memberId;
  }

  /** 도메인 역할 enum. 권한 문자열이 아닌 타입 안전한 비교가 필요할 때 사용한다. */
  public Role getRole() {
    return role;
  }

  /**
   * Spring Security가 권한 검사 시 참조하는 컬렉션.
   *
   * <p>{@link org.springframework.security.config.annotation.web.builders.HttpSecurity}의 {@code
   * hasRole()}, {@link org.springframework.security.access.prepost.PreAuthorize}의 {@code
   * hasRole('ADMIN')} 등이 이 목록과 대조한다.
   */
  //이 사용자가 가진 권한 목록을 Spring Security에 넘겨주는 메서드
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  /**
   * UserDetails 계약상 필수이나, JWT 인증에서는 비밀번호를 SecurityContext에 보관하지 않는다.
   *
   * @return 항상 null (비밀번호 미사용)
   */
  @Override
  public String getPassword() {
    return null;
  }

  /**
   * Spring Security 내부에서 "사용자 이름"으로 쓰이는 식별자.
   *
   * <p>이메일·닉네임이 아니라 memberId 문자열을 반환한다. 로그·감사 추적 시 주의할 것.
   */
  @Override
  public String getUsername() {
    return memberId.toString();
  }

  /** 계정 만료 여부. JWT 만료는 토큰 검증 단계에서 처리하므로 항상 활성으로 둔다. */
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  /** 계정 잠금 여부. 별도 잠금 정책이 없으면 항상 잠기지 않은 상태로 둔다. */
  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  /** 자격 증명(비밀번호) 만료 여부. JWT만 사용하므로 검사하지 않는다. */
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  /** 계정 활성화 여부. 탈퇴·정지 회원은 토큰 발급 단계에서 막는 것이 일반적이다. */
  @Override
  public boolean isEnabled() {
    return true;
  }
}
