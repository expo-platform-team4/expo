package com.expo.expo.domain;

import java.time.OffsetDateTime;

/**
 * V1 스키마의 CHECK 제약과 1:1 로 맞춘 상태 enum 모음.
 * 값을 추가·변경하려면 반드시 DB CHECK 제약 마이그레이션과 함께 진행할 것.
 */
public final class ExpoEnums {

    private ExpoEnums() {}

    /** expo_opening_requests.status — 개최 신청 흐름 (희-EXPO-01/02/17) */
    public enum OpeningRequestStatus {
        DRAFT,
        SUBMITTED,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        CANCELED;

        /** 클라이언트 직접 수정 가능 여부 (희-EXPO-05 / 승인 후 차단 희-EXPO-06) */
        public boolean isEditableByClient() {
            return this == DRAFT || this == SUBMITTED || this == UNDER_REVIEW || this == REJECTED;
        }

        public boolean isSubmittable() {
            return this == DRAFT || this == REJECTED;
        }
    }

    /** expos.review_status */
    public enum ReviewStatus {
        DRAFT,
        UNDER_REVIEW,
        REJECTED,
        APPROVED
    }

    /** expos.visibility_status — 승인 시 PUBLIC 으로 자동 공개 (희-EXPO-09) */
    public enum VisibilityStatus {
        PRIVATE,
        PUBLIC,
        ARCHIVED
    }

    /** expos.event_status */
    public enum EventStatus {
        SCHEDULED,
        ONGOING,
        ENDED,
        CANCELED
    }

    /**
     * 판매 상태 배지 (희-EXPO-10, 희-SRCH-06, 희-SRCH-13)
     * V1 은 sales_start_at / sales_end_at 이 실제 컬럼이므로 판매기간을 그대로 사용하고,
     * 매진 여부만 ticket_inventories.available_quantity 로 판정하는 파생 값.
     */
    public enum SaleStatus {
        UPCOMING("판매예정"),
        ON_SALE("판매중"),
        SOLD_OUT("매진"),
        SALE_ENDED("판매종료"),
        EVENT_ENDED("행사종료");

        private final String label;

        SaleStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        /** 희-EXPO-10: 판매 시작·종료일 기준 자동 계산 (SQL CASE 식과 동일 규칙) */
        public static SaleStatus calculate(
                OffsetDateTime now,
                OffsetDateTime eventEndAt,
                OffsetDateTime salesStartAt,
                OffsetDateTime salesEndAt,
                EventStatus eventStatus,
                boolean hasProducts,
                boolean allSoldOut) {
            if (eventStatus == EventStatus.ENDED
                    || (eventEndAt != null && now.isAfter(eventEndAt))) {
                return EVENT_ENDED;
            }
            if (salesStartAt != null && now.isBefore(salesStartAt)) {
                return UPCOMING; // 희-SRCH-13 판매기간 전 `판매 예정`
            }
            if (salesEndAt != null && now.isAfter(salesEndAt)) {
                return SALE_ENDED;
            }
            if (hasProducts && allSoldOut) {
                return SOLD_OUT;
            }
            return ON_SALE;
        }
    }

    /** expo_images.image_type */
    public enum ExpoImageType {
        THUMBNAIL,
        DETAIL,
        GALLERY
    }

    /** expo_files.file_purpose (희-EXPO-14/15) */
    public enum ExpoFilePurpose {
        INTRO_PDF,
        CATALOG,
        LEAFLET,
        PROMO_VIDEO,
        OTHER
    }

    /** external_links.link_type (희-EXPO-13) */
    public enum ExternalLinkType {
        HOMEPAGE,
        SOCIAL,
        RESERVATION,
        PRODUCT,
        OTHER
    }
}
