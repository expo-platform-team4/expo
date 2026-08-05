package com.expo.jwt;

import com.expo.auth.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** JWT 인증 성공 시 SecurityContext 에 저장되는 인증 주체. */
public class AuthPrincipal implements UserDetails {

  private final Long memberId;
  private final Role role;
  private final List<SimpleGrantedAuthority> authorities;

  public AuthPrincipal(Long memberId, Role role) {
    this.memberId = memberId;
    this.role = role;
    this.authorities = List.of(new SimpleGrantedAuthority(role.getAuthority()));
  }

  public Long getMemberId() {
    return memberId;
  }

  public Role getRole() {
    return role;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return null;
  }

  @Override
  public String getUsername() {
    return memberId.toString();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
