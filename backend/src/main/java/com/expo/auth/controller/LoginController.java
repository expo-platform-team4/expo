package com.expo.auth.controller;

import com.expo.auth.dto.AuthApiResponse;
import com.expo.auth.dto.LoginRequest;
import com.expo.auth.dto.LoginResponse;
import com.expo.auth.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 이메일·비밀번호 로그인 API (A-API-008). {@link AuthController} 와 분리한다. */
@Tag(name = "Login", description = "로그인 API")
@RestController
@RequestMapping("/api/auth")
public class LoginController {

  private final LoginService loginService;

  public LoginController(LoginService loginService) {
    this.loginService = loginService;
  }

  @Operation(
      summary = "이메일·비밀번호 로그인",
      description =
          """
          이메일과 비밀번호로 로컬 로그인합니다.

          - 성공 시 JWT Access Token과 Refresh Token을 발급합니다.
          - Refresh Token 해시는 refresh_tokens 테이블에 저장합니다.
          - 탈퇴(WITHDRAWN) 계정·소셜 전용 계정(password_hash NULL)은 로그인할 수 없습니다.
          """)
  @PostMapping("/login")
  public ResponseEntity<AuthApiResponse<LoginResponse>> login(
      @Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(AuthApiResponse.ok(loginService.login(request)));
  }
}
