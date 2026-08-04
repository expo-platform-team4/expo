package com.expo.auth;
//회원 역할 3가지(MEMBER, CLIENT, ADMIN)를 정의하고, Spring Security 권한 문자열로 바꿔 주는 enum
/** JWT 및 Spring Security 에서 사용하는 회원 역할. */
public enum Role {
  MEMBER,
  CLIENT,
  ADMIN;

  /** Spring Security 권한 문자열 (예: ROLE_MEMBER). */
  public String getAuthority() {
    return "ROLE_" + name();
  }
}
