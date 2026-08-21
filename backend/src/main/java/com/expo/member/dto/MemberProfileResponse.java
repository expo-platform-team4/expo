package com.expo.member.dto;

import com.expo.auth.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 일반 회원·클라이언트 공통 마이페이지 프로필 조회 응답 (A-API-015). */
@Schema(description = "내 프로필")
public record MemberProfileResponse(
        @Schema(description = "사용자 ID") Long userId,
        @Schema(description = "이메일") String email,
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "권한") Role role,
        @Schema(description = "휴대폰 번호") String phoneNumber,
        @Schema(description = "프로필 이미지 파일 ID (없으면 null)") Long profileImageFileId,
        @Schema(description = "프로필 이미지 수정 시각") Instant profileImageUpdatedAt,
        @Schema(description = "가입일") Instant createdAt) {}
