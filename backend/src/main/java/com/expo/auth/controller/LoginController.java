package com.expo.auth.controller;

import com.expo.auth.dto.LoginRequest;
import com.expo.auth.dto.LoginResponse;
import com.expo.auth.dto.TokenReissueRequest;
import com.expo.auth.dto.TokenReissueResponse;
import com.expo.auth.service.LoginService;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 이메일·비밀번호 로그인·토큰 재발급 API (A-API-008, A-API-011). {@link AuthController} 와 분리한다. */

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
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(loginService.login(request)));
    }

    @Operation(
            summary = "Access Token 재발급",
            description =
                    """
          Refresh Token으로 새 Access Token을 발급합니다.

          - 로그인 시 받은 Refresh Token 원문이 필요합니다.
          - DB에 저장된 해시·만료·폐기 상태를 검증합니다.
          - 성공 시 refresh_tokens.last_used_at을 갱신합니다.
          """)
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenReissueResponse>> reissueAccessToken(
            @Valid @RequestBody TokenReissueRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(loginService.reissueAccessToken(request.refreshToken())));
    }

    @Operation(
            summary = "전체 기기 로그아웃",
            description =
                    """
          로그인한 사용자 명의로 발급된 모든 Refresh Token을 폐기합니다.

          - Authorization 헤더에 유효한 Access Token이 필요합니다.
          - Access Token 자체는 Stateless라 서버에서 즉시 무효화되지 않고, 클라이언트가 폐기해야 합니다.
          - 이후 폐기된 Refresh Token으로는 재발급(A-API-011)을 시도할 수 없습니다.
          """)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        loginService.logout(principal.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
