package com.expo.banner.dto;

import com.expo.banner.entity.Banner;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 현재 노출 가능한 메인 배너 응답 (B-API-024: GET /api/banners/active)
 *
 * 누구나 조회 가능한 공개 API 이므로 신청자·심사 관련 내부 정보는 담지 않는다.
 */
@Schema(description = "현재 노출 중인 메인 배너")
public record ActiveBannerResponse(
        @Schema(description = "배너 ID") Long id,
        @Schema(description = "홍보 대상 박람회 ID") Long expoId,
        @Schema(description = "배너 이미지 파일 ID") Long imageFileId,
        @Schema(description = "배너 문구") String headline,
        @Schema(description = "노출 순서") Integer sortOrder) {

    public static ActiveBannerResponse from(Banner banner) {
        return new ActiveBannerResponse(
                banner.getId(),
                banner.getExpoId(),
                banner.getImageFileId(),
                banner.getHeadline(),
                banner.getSortOrder());
    }
}
