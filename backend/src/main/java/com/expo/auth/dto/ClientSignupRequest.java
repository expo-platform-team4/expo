package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 클라이언트 로컬 회원가입 요청 (A-API-002).
 *
 * <p>프론트 회원가입 화면(담당자 정보 + 기업 정보 + 약관) 기준.
 */
@Schema(description = "클라이언트 로컬 회원가입 요청")
public record ClientSignupRequest(
        // ----- 담당자 정보 -----
        @Schema(description = "이메일(아이디)", example = "client@espotic.com")
                @NotBlank(message = "이메일은 필수입니다.")
                @Email(message = "올바른 이메일 형식이 아닙니다.")
                @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
                String email,
        @Schema(description = "비밀번호 (영문·숫자·특수문자 조합 8자 이상)", example = "Test1234!")
                @NotBlank(message = "비밀번호는 필수입니다.")
                @Pattern(
                        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$",
                        message = "비밀번호는 영문·숫자·특수문자를 포함해 8자 이상이어야 합니다.")
                String password,
        @Schema(description = "비밀번호 확인", example = "Test1234!")
                @NotBlank(message = "비밀번호 확인은 필수입니다.")
                String passwordConfirm,
        @Schema(description = "담당자 닉네임", example = "espotic_manager")
                @NotBlank(message = "닉네임은 필수입니다.")
                @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하여야 합니다.")
                String nickname,
        @Schema(description = "담당자 휴대폰 번호 (- 제외 숫자만)", example = "01012345678")
                @NotBlank(message = "휴대폰 번호는 필수입니다.")
                @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
                String phoneNumber,
        @Schema(
                        description =
                                "이메일 인증 완료 토큰 (POST /api/auth/email-verifications/confirm 응답값)",
                        example = "7f3c2a1b-9d4e-5f6a-8b7c-1d2e3f4a5b6c")
                @NotBlank(message = "이메일 인증을 완료해 주세요.")
                String emailVerificationToken,
        @Schema(
                        description =
                                "휴대폰 인증 완료 토큰 (POST /api/auth/phone-verifications/confirm 응답값)",
                        example = "33653ff9-35ba-424a-b366-cb0caf958afa")
                @NotBlank(message = "휴대폰 본인인증을 완료해 주세요.")
                String phoneVerificationToken,

        // ----- 기업 정보 -----
        @Schema(description = "정식 기업명", example = "주식회사 에스포틱")
                @NotBlank(message = "기업명은 필수입니다.")
                @Size(max = 150, message = "기업명은 150자 이하여야 합니다.")
                String companyName,
        @Schema(description = "사업자등록번호 (하이픈 포함 가능)", example = "1234567890")
                @NotBlank(message = "사업자등록번호를 입력해 주세요.")
                @Pattern(regexp = "^\\d{3}-?\\d{2}-?\\d{5}$", message = "사업자등록번호 형식이 올바르지 않습니다.")
                String businessNumber,

        // ----- 약관 동의 -----
        @Schema(description = "[필수] 서비스 이용약관 동의", example = "true")
                @AssertTrue(message = "서비스 이용약관에 동의해 주세요.")
                boolean serviceTermsAgreed,
        @Schema(description = "[필수] 개인정보 수집 및 이용 동의", example = "true")
                @AssertTrue(message = "개인정보 수집 및 이용에 동의해 주세요.")
                boolean privacyPolicyAgreed,
        @Schema(description = "[선택] 마케팅 정보 수신 동의", example = "false") boolean marketingAgreed) {}
