package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "휴대폰 본인인증 요청")
public record PhoneVerificationCreateRequest(
    @Schema(description = "휴대폰 번호 (하이픈 포함·미포함 허용)", example = "01012345678")
        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        String phoneNumber) {}
