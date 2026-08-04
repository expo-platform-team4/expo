package com.expo.auth.controller;

import com.expo.auth.dto.AuthApiResponse;
import com.expo.auth.dto.SignupRequest;
import com.expo.auth.dto.SignupResponse;
import com.expo.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @Operation(summary = "일반 회원 로컬 회원가입", description = "이메일·비밀번호·닉네임으로 MEMBER 계정을 생성합니다.")
  @PostMapping("/signup")
  public ResponseEntity<AuthApiResponse<SignupResponse>> signup(
      @Valid @RequestBody SignupRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(AuthApiResponse.ok(authService.signup(request)));
  }
}
