package com.expo.banner.dto;

import com.expo.banner.entity.BannerApplication;
import com.expo.banner.entity.BannerApplication.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/**
 * 배너 신청 상세/목록 응답 (B-API-020 내 신청 목록, B-API-021/021A 관리자 목록·상세)
 *
 * 신청자 본인과 관리자만 보는 화면이므로 반려 사유 등 내부 정보를 그대로 포함한다.
 * 일반 사용자에게 공개되는 활성 배너 조회(B-API-024)는 ActiveBannerResponse 를 따로 쓴다.
 */
@Schema(description = "배너 신청 상세/목록 응답")
public record BannerApplicationResponse(
        @Schema(description = "배너 신청 ID") Long id,
        @Schema(description = "신청 클라이언트 ID") Long clientUserId,
        @Schema(description = "홍보 대상 박람회 ID") Long expoId,
        @Schema(description = "배너 이미지 파일 ID") Long imageFileId,
        @Schema(description = "배너 문구") String headline,
        @Schema(description = "희망 노출 시작일시") OffsetDateTime requestedStartAt,
        @Schema(description = "희망 노출 종료일시") OffsetDateTime requestedEndAt,
        @Schema(description = "심사 상태") ReviewStatus reviewStatus,
        @Schema(description = "심사 제출 시각") OffsetDateTime submittedAt,
        @Schema(description = "심사한 관리자 ID") Long reviewedByAdminId,
        @Schema(description = "심사 처리 시각") OffsetDateTime reviewedAt,
        @Schema(description = "반려 사유") String rejectionReason,
        @Schema(description = "생성 시각") OffsetDateTime createdAt,
        @Schema(description = "수정 시각") OffsetDateTime updatedAt) {

    public static BannerApplicationResponse from(BannerApplication application) {
        return new BannerApplicationResponse(
                application.getId(),
                application.getClientUserId(),
                application.getExpoId(),
                application.getImageFileId(),
                application.getHeadline(),
                application.getRequestedStartAt(),
                application.getRequestedEndAt(),
                application.getReviewStatus(),
                application.getSubmittedAt(),
                application.getReviewedByAdminId(),
                application.getReviewedAt(),
                application.getRejectionReason(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
