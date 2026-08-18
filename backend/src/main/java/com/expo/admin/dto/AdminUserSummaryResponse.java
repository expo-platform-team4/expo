package com.expo.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 회원·클라이언트 계정 검색·목록 응답 ({@code v_admin_users} 뷰 기반, E-API-012).
 *
 * <p>목록에서는 검색·판별에 필요한 최소 필드만 노출한다. 사업자 상세는 {@link AdminUserDetailResponse}(E-API-013)에서
 * 조회한다.
 */
@Schema(description = "관리자 - 계정 목록")
public record AdminUserSummaryResponse(
        @Schema(description = "사용자 ID") Long userId,
        @Schema(description = "이메일") String email,
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "역할", example = "CLIENT") String role,
        @Schema(description = "계정 상태") String accountStatus,
        @Schema(description = "회사명 (CLIENT만 존재)") String companyName,
        @Schema(description = "가입 시각") Instant createdAt,
        @Schema(description = "최근 로그인 시각") Instant lastLoginAt) {}
