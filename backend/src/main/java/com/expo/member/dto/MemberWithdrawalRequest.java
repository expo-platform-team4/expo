package com.expo.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 회원 탈퇴 요청 (A-API-018). 본인 확인용 현재 비밀번호를 받는다. */
@Schema(description = "회원 탈퇴 요청")
public record MemberWithdrawalRequest(
        @Schema(description = "현재 비밀번호") @NotBlank(message = "비밀번호는 필수입니다.") String password) {}
