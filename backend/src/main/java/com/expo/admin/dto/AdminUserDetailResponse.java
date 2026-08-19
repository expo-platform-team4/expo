package com.expo.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 계정·사업자 프로필·상태 상세 응답 ({@code v_admin_users} 뷰 기반, E-API-013). */
@Schema(description = "관리자 - 계정 상세")
public record AdminUserDetailResponse(
        @Schema(description = "사용자 ID") Long userId,
        @Schema(description = "이메일") String email,
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "역할", example = "CLIENT") String role,
        @Schema(description = "계정 상태") String accountStatus,
        @Schema(description = "휴대폰 번호") String phoneNumber,
        @Schema(description = "휴대폰 인증 시각") Instant phoneVerifiedAt,
        @Schema(description = "최근 로그인 시각") Instant lastLoginAt,
        @Schema(description = "탈퇴 시각") Instant withdrawnAt,
        @Schema(description = "가입 시각") Instant createdAt,
        @Schema(description = "회사명 (CLIENT만 존재)") String companyName,
        @Schema(description = "사업자등록번호 (CLIENT만 존재)") String businessNumber,
        @Schema(description = "대표자명 (CLIENT만 존재)") String representativeName,
        @Schema(description = "사업장 주소 (CLIENT만 존재)") String businessAddress,
        @Schema(description = "업태·업종 (CLIENT만 존재)") String businessType,
        @Schema(description = "사업자등록번호 검증 여부") Boolean businessNumberVerified,
        @Schema(description = "사업자 검증 시각") Instant businessVerifiedAt,
        @Schema(description = "검증 제공처") String verificationProvider) {}
