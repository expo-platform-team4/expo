package com.expo.auth.controller;

import com.expo.auth.dto.PasswordResetConfirmRequest;
import com.expo.auth.dto.PasswordResetConfirmResponse;
import com.expo.auth.dto.PasswordResetRequest;
import com.expo.auth.dto.PasswordResetRequestResponse;
import com.expo.auth.service.PasswordResetService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 비밀번호 재설정 API (A-API-013, A-API-014). {@link AuthController}와 분리한다. */
@Tag(name = "Password Reset", description = "비밀번호 재설정 API")
@RestController
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @Operation(
            summary = "비밀번호 재설정 요청",
            description =
                    """
          가입한 이메일로 비밀번호 재설정을 요청합니다.

          - 계정 존재 여부를 응답으로 노출하지 않습니다. 가입되지 않은 이메일이어도 동일한 성공 메시지를 반환합니다.
          - `app.mail.provider=smtp` 면 재설정 코드를 실제 메일로 보냅니다. 기본값(logging)이면 발송하지
            않고 서버 로그에만 남습니다.
          - 재요청 시 앞서 발급된 토큰은 폐기됩니다.
          """)
    @PostMapping("/api/auth/password-reset-requests")
    public ResponseEntity<ApiResponse<PasswordResetRequestResponse>> requestReset(
            @Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(passwordResetService.requestReset(request.email())));
    }

    @Operation(
            summary = "재설정 토큰 확인 및 새 비밀번호 저장",
            description =
                    """
          재설정 요청 API가 발급한 토큰과 새 비밀번호로 비밀번호를 변경합니다.

          - 토큰은 30분간 유효하며, 한 번 쓰면 다시 쓸 수 없습니다.
          """)
    @PostMapping("/api/auth/password-resets/confirm")
    public ResponseEntity<ApiResponse<PasswordResetConfirmResponse>> confirmReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        passwordResetService.confirmReset(
                                request.resetToken(),
                                request.newPassword(),
                                request.newPasswordConfirm())));
    }
}
