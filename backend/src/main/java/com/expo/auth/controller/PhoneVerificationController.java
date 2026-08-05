package com.expo.auth.controller;

import com.expo.auth.dto.AuthApiResponse;
import com.expo.auth.dto.PhoneVerificationCreateRequest;
import com.expo.auth.dto.PhoneVerificationCreateResponse;
import com.expo.auth.service.PhoneVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 휴대폰 본인인증 API (A-API-006). {@link AuthController} 와 분리한다. */
@Tag(name = "Phone Verification", description = "휴대폰 본인인증 API")
@RestController
@RequestMapping("/api/auth/phone-verifications")
public class PhoneVerificationController {

  private final PhoneVerificationService phoneVerificationService;

  public PhoneVerificationController(PhoneVerificationService phoneVerificationService) {
    this.phoneVerificationService = phoneVerificationService;
  }

  @Operation(
      summary = "휴대폰 본인인증 요청",
      description =
          """
          휴대폰 번호로 본인인증을 요청합니다.

          - 동일 번호로 재요청 시 기존 REQUESTED 건은 EXPIRED 로 만료 처리합니다.
          - 인증번호 유효 시간은 기본 3분입니다 (환경변수로 변경 가능).
          - MVP 단계: 외부 SMS API 미연동. 테스트용 고정 인증번호 `123456`으로 검증합니다.
          """)
  @PostMapping
  public ResponseEntity<AuthApiResponse<PhoneVerificationCreateResponse>> requestVerification(
      @Valid @RequestBody PhoneVerificationCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            AuthApiResponse.ok(
                phoneVerificationService.requestVerification(request.phoneNumber())));
  }
}
