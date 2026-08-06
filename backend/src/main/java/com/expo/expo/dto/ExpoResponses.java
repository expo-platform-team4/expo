package com.expo.expo.dto;

import com.expo.expo.domain.*;
import com.expo.expo.repository.ExpoCardProjection;

import java.time.LocalDate;
import java.util.List;

/**
 * 박람회 도메인 Response DTO 모음
 */
public final class ExpoResponses {

    private ExpoResponses() {
    }

    /**
     * 목록 카드 응답 (희-SRCH-11: 제목·기간·장소·가격·상태 표시)
     */
    public record ExpoCardResponse(
        Long expoId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String region,
        String venueName,
        String thumbnailUrl,
        Integer minPrice,          // null이면 티켓 미등록(판매 예정)
        String priceLabel,         // 예: "15,000원부터"
        SaleStatus saleStatus,
        String saleStatusLabel     // 판매예정/판매중/매진/행사종료 배지
    ) {
        public static ExpoCardResponse from(ExpoCardProjection p) {
            SaleStatus saleStatus = SaleStatus.valueOf(p.getSaleStatus());
            String priceLabel = p.getMinPrice() == null
                ? "가격 미정"
                : String.format("%,d원부터", p.getMinPrice());
            return new ExpoCardResponse(
                p.getExpoId(), p.getTitle(), p.getStartDate(), p.getEndDate(),
                p.getRegion(), p.getVenueName(), p.getThumbnailUrl(),
                p.getMinPrice(), priceLabel, saleStatus, saleStatus.getLabel()
            );
        }
    }

    /** 상세 응답 */
    public record ExpoDetailResponse(
        Long expoId,
        Long clientId,
        Long categoryId,
        String desiredVenue,
        Long venueId,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String thumbnailUrl,
        String region,
        ExpoStatus status,
        SaleStatus saleStatus,
        List<TicketTypeResponse> ticketTypes,
        List<ExpoFileResponse> files
    ) {
        // 반려 사유·심사요청/처리/공개 일시는 심사 이력 도메인(타 담당) API에서 제공
        public static ExpoDetailResponse of(Expo expo,
                                            SaleStatus saleStatus,
                                            List<TicketType> ticketTypes,
                                            List<ExpoFile> files) {
            return new ExpoDetailResponse(
                expo.getExpoId(), expo.getClientId(), expo.getCategoryId(),
                expo.getDesiredVenue(), expo.getVenueId(), expo.getTitle(),
                expo.getDescription(), expo.getStartDate(), expo.getEndDate(),
                expo.getThumbnailUrl(), expo.getRegion(), expo.getStatus(),
                saleStatus,
                ticketTypes.stream().map(TicketTypeResponse::from).toList(),
                files.stream().map(ExpoFileResponse::from).toList()
            );
        }
    }

    /** 티켓 종류 응답 */
    public record TicketTypeResponse(
        Long ticketTypeId,
        String name,
        Integer price,
        Integer totalQuantity,
        Integer soldQuantity,
        Integer remainingQuantity,
        boolean soldOut
    ) {
        public static TicketTypeResponse from(TicketType t) {
            return new TicketTypeResponse(
                t.getTicketTypeId(), t.getName(), t.getPrice(),
                t.getTotalQuantity(), t.getSoldQuantity(),
                t.getRemainingQuantity(), t.isSoldOut()
            );
        }
    }

    /** 첨부 자료 응답 */
    public record ExpoFileResponse(
        Long expoFileId,
        ExpoFileType fileType,
        String url,
        String displayName
    ) {
        public static ExpoFileResponse from(ExpoFile f) {
            return new ExpoFileResponse(f.getExpoFileId(), f.getFileType(), f.getUrl(), f.getDisplayName());
        }
    }
}

