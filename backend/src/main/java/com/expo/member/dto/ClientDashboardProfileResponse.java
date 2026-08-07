package com.expo.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 클라이언트 마이페이지 상단 프로필 응답 ({@code v_client_dashboard_profile} 뷰 기반).
 *
 * <p>{@code profileImageStorageKey}는 원본 스토리지 키이며, 서명 URL 생성은 애플리케이션이 담당한다.
 */
@Schema(description = "클라이언트 마이페이지 프로필")
public record ClientDashboardProfileResponse(
        @Schema(description = "클라이언트 사용자 ID") Long clientUserId,
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "회사명") String companyName,
        @Schema(description = "프로필 이미지 파일 ID") Long profileImageFileId,
        @Schema(description = "프로필 이미지 스토리지 키") String profileImageStorageKey,
        @Schema(description = "프로필 이미지 수정 시각") Instant profileImageUpdatedAt) {}
