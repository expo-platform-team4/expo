package com.expo.auth.controller;

import com.expo.auth.dto.EmailVerificationConfirmRequest;
import com.expo.auth.dto.EmailVerificationConfirmResponse;
import com.expo.auth.dto.EmailVerificationCreateRequest;
import com.expo.auth.dto.EmailVerificationCreateResponse;
import com.expo.auth.service.EmailVerificationService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 이메일 본인인증 API. {@link AuthController} 와 분리한다({@link PhoneVerificationController}와 같은 이유). */
@Tag(name = "Email Verification", description = "회원가입 이메일 본인인증 API")
@RestController
@RequestMapping("/api/auth/email-verifications")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @Operation(
            summary = "이메일 본인인증 요청",
            description =
                    """
          입력한 이메일로 6자리 인증코드를 보냅니다.

          - 동일 이메일로 재요청 시 기존 REQUESTED 건은 EXPIRED 로 만료 처리합니다.
          - 인증코드 유효 시간은 기본 5분입니다 (환경변수로 변경 가능).
          - `app.mail.provider=smtp` 여야 실제로 메일이 나갑니다. 기본값(logging)이면 서버 로그에만 남습니다.
          """)
    @PostMapping
    public ResponseEntity<ApiResponse<EmailVerificationCreateResponse>> requestVerification(
            @Valid @RequestBody EmailVerificationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                emailVerificationService.requestVerification(request.email())));
    }

    @Operation(
            summary = "이메일 본인인증 결과 확인",
            description =
                    """
          받은 인증코드를 검증하고 본인인증을 완료합니다.

          - 요청 API에서 받은 verificationId와 6자리 인증코드가 필요합니다.
          - 성공 시 회원가입 API(`emailVerificationToken`)에서 사용할 토큰을 발급합니다. 이 토큰은
            인증을 요청한 이메일로 가입할 때만 유효하며, 1회만 쓸 수 있습니다.
          """)
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<EmailVerificationConfirmResponse>> confirmVerification(
            @Valid @RequestBody EmailVerificationConfirmRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        emailVerificationService.confirmVerification(
                                request.verificationId(), request.verificationCode())));
    }
}
