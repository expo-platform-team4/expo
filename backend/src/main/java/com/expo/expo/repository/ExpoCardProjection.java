package com.expo.expo.repository;

import java.time.OffsetDateTime;

/**
 * 목록 카드 Projection (희-SRCH-11: 제목·기간·장소·가격·상태 표시)
 * 썸네일은 file_metadata 의 storage_key 를 반환하며,
 * 실제 노출 URL 은 스토리지 서비스(S3 presigned 등)에서 해석한다.
 */
public interface ExpoCardProjection {
    Long getExpoId();

    String getTitle();

    OffsetDateTime getEventStartAt();

    OffsetDateTime getEventEndAt();

    OffsetDateTime getSalesStartAt();

    OffsetDateTime getSalesEndAt();

    String getRegionCode();

    String getVenueName(); // 확정 장소명 (배정 전이면 null)

    Integer getMinPrice(); // "15,000원부터" 표기용 MIN(ticket_products.price)

    Long getThumbnailFileId();

    String getThumbnailStorageKey();

    String getSaleStatus(); // 파생 판매 상태 (희-EXPO-10)
}
