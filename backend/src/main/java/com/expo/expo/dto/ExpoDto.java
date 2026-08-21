package com.expo.expo.dto;

import com.expo.expo.entity.Expo;
import com.expo.expo.entity.ExpoAttachments.ExpoFile;
import com.expo.expo.entity.ExpoAttachments.ExpoImage;
import com.expo.expo.entity.ExpoAttachments.ExternalLink;
import com.expo.expo.entity.ExpoEnums.EventStatus;
import com.expo.expo.entity.ExpoEnums.ExpoFilePurpose;
import com.expo.expo.entity.ExpoEnums.ExpoImageType;
import com.expo.expo.entity.ExpoEnums.ExternalLinkType;
import com.expo.expo.entity.ExpoEnums.OpeningRequestStatus;
import com.expo.expo.entity.ExpoEnums.ReviewStatus;
import com.expo.expo.entity.ExpoEnums.SaleStatus;
import com.expo.expo.entity.ExpoEnums.VisibilityStatus;
import com.expo.expo.entity.ExpoOpeningRequest;
import com.expo.expo.repository.ExpoCardProjection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 박람회 도메인 DTO 모음 (V1 스키마 기준)
 */
public final class ExpoDto {

    private ExpoDto() {}

    /* ==================== Request ==================== */

    /**
     * 개최 신청 임시저장/생성 (희-EXPO-01, 희-EXPO-17)
     * V1 스키마 제약상 제목·소개·행사/판매 일시는 DRAFT 에서도 필수(NOT NULL).
     * 신청자(host)는 인증 주체(AuthPrincipal)에서 받으므로 요청 필드에 두지 않는다.
     */
    public record OpeningRequestCreate(
            @NotBlank @Size(max = 255) String title,
            @NotBlank String description,
            @NotNull OffsetDateTime eventStartAt,
            @NotNull OffsetDateTime eventEndAt,
            @NotNull OffsetDateTime salesStartAt,
            @NotNull OffsetDateTime salesEndAt,
            Long desiredVenueId,
            Long desiredVenueHallId,
            Long desiredVenueZoneId,
            List<Long> categoryIds) {}

    /** 승인 전 직접 수정 (희-EXPO-05) — null 필드는 유지 */
    public record OpeningRequestUpdate(
            @Size(max = 255) String title,
            String description,
            OffsetDateTime eventStartAt,
            OffsetDateTime eventEndAt,
            OffsetDateTime salesStartAt,
            OffsetDateTime salesEndAt,
            Long desiredVenueId,
            Long desiredVenueHallId,
            Long desiredVenueZoneId) {}

    /**
     * 승인 (희-EXPO-09) — expos 생성 + 자동 공개.
     * regionCode 미지정 시 희망 장소(virtual_venues.region_code)에서 해석.
     * 처리 관리자는 인증 주체에서 받으므로 요청 필드에 두지 않는다.
     */
    public record OpeningRequestApprove(
            @Size(max = 30) String regionCode, List<Long> categoryIds) {}

    public record OpeningRequestReject(@NotBlank String rejectionReason) {}

    /** 썸네일·상세 이미지 등록 (희-EXPO-15: THUMBNAIL = 대표 이미지) */
    public record ExpoImageCreate(
            @NotNull Long fileId,
            @NotNull ExpoImageType imageType,
            @Size(max = 255) String altText,
            Integer sortOrder) {}

    /** PDF·카탈로그·리플렛·홍보영상 등록 (희-EXPO-14/15) */
    public record ExpoFileCreate(
            @NotNull Long fileId,
            @NotNull ExpoFilePurpose filePurpose,
            @Size(max = 150) String title,
            Integer sortOrder) {}

    /** 외부 링크 등록 (희-EXPO-13) */
    public record ExternalLinkCreate(
            @NotNull ExternalLinkType linkType,
            @Size(max = 100) String label,
            @NotBlank @Size(max = 1000) String url,
            Integer sortOrder) {}

    /** 승인 후 수정 요청 (희-EXPO-06 대응 경로) */
    public record ChangeRequestCreate(
            @NotNull Long requesterClientId,
            @NotBlank String changeReason,
            @NotNull Map<String, Object> requestedChanges) {}

    /* ==================== 검색 조건 (희-SRCH-01~10) ==================== */

    public record SearchCondition(
            String keyword, // 01 제목
            Long categoryId, // 02 카테고리 (expo_categories)
            String regionCode, // 03 지역 (expos.region_code)
            LocalDate fromDate, // 04 기간
            LocalDate toDate,
            Integer minPrice, // 05 가격 (MIN(ticket_products.price))
            Integer maxPrice,
            SaleStatus saleStatus, // 06 판매 상태
            Sort sort // 07~09 정렬
            ) {
        public enum Sort {
            /** 최신순 — approved_at DESC (승인=공개 시각, 희-SRCH-07) */
            LATEST,
            /** 마감 임박순 — sales_end_at ASC (희-SRCH-08) */
            DEADLINE,
            /** 인기순 — SUM(sold_quantity) DESC (희-SRCH-09) */
            POPULAR
        }

        public Sort sortOrDefault() {
            return sort == null ? Sort.LATEST : sort;
        }
    }

    /* ==================== Response ==================== */

    /** 목록 카드 (희-SRCH-11) */
    public record ExpoCardResponse(
            Long expoId,
            String title,
            OffsetDateTime eventStartAt,
            OffsetDateTime eventEndAt,
            String regionCode,
            String venueName,
            Integer minPrice,
            String priceLabel,
            Long thumbnailFileId,
            String thumbnailStorageKey,
            SaleStatus saleStatus,
            String saleStatusLabel) {
        public static ExpoCardResponse from(ExpoCardProjection p) {
            SaleStatus s = SaleStatus.valueOf(p.getSaleStatus());
            String priceLabel =
                    p.getMinPrice() == null ? "가격 미정" : String.format("%,d원부터", p.getMinPrice());
            return new ExpoCardResponse(
                    p.getExpoId(),
                    p.getTitle(),
                    p.getEventStartAt(),
                    p.getEventEndAt(),
                    p.getRegionCode(),
                    p.getVenueName(),
                    p.getMinPrice(),
                    priceLabel,
                    p.getThumbnailFileId(),
                    p.getThumbnailStorageKey(),
                    s,
                    s.getLabel());
        }
    }

    /** 개최 신청 응답 */
    public record OpeningRequestResponse(
            Long id,
            Long hostClientId,
            String title,
            String description,
            OffsetDateTime eventStartAt,
            OffsetDateTime eventEndAt,
            OffsetDateTime salesStartAt,
            OffsetDateTime salesEndAt,
            Long desiredVenueId,
            Long desiredVenueHallId,
            Long desiredVenueZoneId,
            OpeningRequestStatus status,
            OffsetDateTime submittedAt,
            OffsetDateTime reviewedAt,
            String rejectionReason) {
        public static OpeningRequestResponse from(ExpoOpeningRequest r) {
            return new OpeningRequestResponse(
                    r.getId(),
                    r.getHostClientId(),
                    r.getTitle(),
                    r.getDescription(),
                    r.getEventStartAt(),
                    r.getEventEndAt(),
                    r.getSalesStartAt(),
                    r.getSalesEndAt(),
                    r.getDesiredVenueId(),
                    r.getDesiredVenueHallId(),
                    r.getDesiredVenueZoneId(),
                    r.getStatus(),
                    r.getSubmittedAt(),
                    r.getReviewedAt(),
                    r.getRejectionReason());
        }
    }

    /** 공개 박람회 상세 응답 */
    public record ExpoDetailResponse(
            Long id,
            Long hostClientId,
            Long openingRequestId,
            String title,
            String description,
            String regionCode,
            OffsetDateTime eventStartAt,
            OffsetDateTime eventEndAt,
            OffsetDateTime salesStartAt,
            OffsetDateTime salesEndAt,
            ReviewStatus reviewStatus,
            VisibilityStatus visibilityStatus,
            EventStatus eventStatus,
            SaleStatus saleStatus,
            List<ImageResponse> images,
            List<FileResponse> files,
            List<LinkResponse> links) {
        public static ExpoDetailResponse of(
                Expo e,
                SaleStatus saleStatus,
                List<ExpoImage> images,
                List<ExpoFile> files,
                List<ExternalLink> links) {
            return new ExpoDetailResponse(
                    e.getId(),
                    e.getHostClientId(),
                    e.getOpeningRequestId(),
                    e.getTitle(),
                    e.getDescription(),
                    e.getRegionCode(),
                    e.getEventStartAt(),
                    e.getEventEndAt(),
                    e.getSalesStartAt(),
                    e.getSalesEndAt(),
                    e.getReviewStatus(),
                    e.getVisibilityStatus(),
                    e.getEventStatus(),
                    saleStatus,
                    images.stream().map(ImageResponse::from).toList(),
                    files.stream().map(FileResponse::from).toList(),
                    links.stream().map(LinkResponse::from).toList());
        }
    }

    public record ImageResponse(
            Long id, Long fileId, ExpoImageType imageType, String altText, Integer sortOrder) {
        public static ImageResponse from(ExpoImage i) {
            return new ImageResponse(
                    i.getId(), i.getFileId(), i.getImageType(), i.getAltText(), i.getSortOrder());
        }
    }

    public record FileResponse(
            Long id, Long fileId, ExpoFilePurpose filePurpose, String title, Integer sortOrder) {
        public static FileResponse from(ExpoFile f) {
            return new FileResponse(
                    f.getId(), f.getFileId(), f.getFilePurpose(), f.getTitle(), f.getSortOrder());
        }
    }

    public record LinkResponse(
            Long id, ExternalLinkType linkType, String label, String url, Integer sortOrder) {
        public static LinkResponse from(ExternalLink l) {
            return new LinkResponse(
                    l.getId(), l.getLinkType(), l.getLabel(), l.getUrl(), l.getSortOrder());
        }
    }
}
