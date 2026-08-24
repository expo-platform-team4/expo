package com.expo.banner.dto;

import com.expo.banner.entity.BannerApplication;
import com.expo.banner.entity.BannerApplication.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/**
 * 관리자 배너 신청 목록 응답 (B-API-021: GET /api/admin/banner-requests)
 *
 * <p>WBS 상 "목록·기간 충돌 조회"가 하나의 엔드포인트로 묶여 있어, 신청 건마다 현재 승인된
 * 배너와 노출 기간이 겹치는지 여부({@code hasPeriodConflict})를 같이 내려준다. 실제로 겹치는
 * 배너 목록까지 필요하면 {@code GET /{requestId}/conflicts} 를 드릴다운으로 호출한다.
 */
@Schema(description = "관리자 배너 신청 목록 항목 (기간 충돌 여부 포함)")
public record BannerApplicationAdminResponse(
        @Schema(description = "배너 신청 ID") Long id,
        @Schema(description = "신청 클라이언트 ID") Long clientUserId,
        @Schema(description = "홍보 대상 박람회 ID") Long expoId,
        @Schema(description = "배너 이미지 파일 ID") Long imageFileId,
        @Schema(description = "배너 문구") String headline,
        @Schema(description = "희망 노출 시작일시") OffsetDateTime requestedStartAt,
        @Schema(description = "희망 노출 종료일시") OffsetDateTime requestedEndAt,
        @Schema(description = "심사 상태") ReviewStatus reviewStatus,
        @Schema(description = "생성 시각") OffsetDateTime createdAt,
        @Schema(description = "이미 승인된 배너와 노출 기간이 겹치는지 여부") boolean hasPeriodConflict) {

    public static BannerApplicationAdminResponse from(
            BannerApplication application, boolean hasPeriodConflict) {
        return new BannerApplicationAdminResponse(
                application.getId(),
                application.getClientUserId(),
                application.getExpoId(),
                application.getImageFileId(),
                application.getHeadline(),
                application.getRequestedStartAt(),
                application.getRequestedEndAt(),
                application.getReviewStatus(),
                application.getCreatedAt(),
                hasPeriodConflict);
    }
}
