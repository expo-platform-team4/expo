package com.expo.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 로그인 상태에서의 비밀번호 변경 요청.
 *
 * <p>이메일 재설정(A-API-013·014, {@code PasswordResetConfirmRequest})과는 다른 흐름이다 — 그건
 * "비밀번호를 잊어버렸을 때" 토큰으로 본인을 증명하고, 이건 이미 로그인된 사람이 현재 비밀번호로
 * 본인을 증명한다. 마이페이지 "프로필 수정"에서는 이쪽만 쓴다.
 */
@Schema(description = "비밀번호 변경 요청 (로그인 상태)")
public record ChangePasswordRequest(
        @Schema(description = "현재 비밀번호") @NotBlank(message = "현재 비밀번호는 필수입니다.")
                String currentPassword,
        @Schema(description = "새 비밀번호 (영문·숫자·특수문자 조합 8자 이상)", example = "NewTest1234!")
                @NotBlank(message = "새 비밀번호는 필수입니다.")
                @Pattern(
                        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$",
                        message = "비밀번호는 영문·숫자·특수문자를 포함해 8자 이상이어야 합니다.")
                String newPassword,
        @Schema(description = "새 비밀번호 확인", example = "NewTest1234!")
                @NotBlank(message = "새 비밀번호 확인은 필수입니다.")
                String newPasswordConfirm) {}
