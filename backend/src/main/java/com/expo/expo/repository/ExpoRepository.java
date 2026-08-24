package com.expo.expo.repository;

import com.expo.expo.entity.Expo;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * ExpoRepository — 공개 박람회(expos) JPA Repository (V1 스키마 기준)
 *
 * <p>검색(희-SRCH-01~13)은 카테고리 N:M, 티켓 최저가/재고 집계, 썸네일, 확정 장소명이 필요해 PostgreSQL 네이티브 쿼리로 구현.
 * 판매 상태(희-EXPO-10)는 V1 의 sales_start_at / sales_end_at 실컬럼과 ticket_inventories.available_quantity 로
 * 계산한다.
 */
public interface ExpoRepository extends JpaRepository<Expo, Long> {

    Optional<Expo> findByOpeningRequestId(Long openingRequestId);

    Page<Expo> findByHostClientId(Long hostClientId, Pageable pageable);

    /** 승인 시 확정 장소의 지역 코드 조회용 (expos.region_code NOT NULL 채우기) */
    @Query(
            value = "SELECT vv.region_code FROM virtual_venues vv WHERE vv.id = :venueId",
            nativeQuery = true)
    Optional<String> findVenueRegionCode(@Param("venueId") Long venueId);

    /**
     * 공개 박람회 목록 검색 (희-SRCH-01 ~ 13)
     *
     * <p>노출 조건(희-SRCH-12): visibility_status='PUBLIC' AND review_status='APPROVED' AND
     * event_status &lt;&gt; 'CANCELED' → 승인 즉시 자동 반영
     *
     * <p>필터: 제목(01) · 카테고리(02) · 지역(03) · 기간(04) · 가격(05) · 판매상태(06)
     *
     * <p>정렬: LATEST(07) · DEADLINE(08) · POPULAR(09)
     *
     * <p>파라미터가 10개인 것은 네이티브 쿼리라 값 객체로 묶을 수 없기 때문이다.
     */
    // spotless:off
    @Query(
            value =
                    """
            SELECT e.id                                   AS expoId,
                   e.title                                AS title,
                   e.event_start_at                       AS eventStartAt,
                   e.event_end_at                         AS eventEndAt,
                   e.sales_start_at                       AS salesStartAt,
                   e.sales_end_at                         AS salesEndAt,
                   e.region_code                          AS regionCode,
                   vv.name                                AS venueName,
                   t.min_price                            AS minPrice,
                   th.file_id                             AS thumbnailFileId,
                   fm.storage_key                         AS thumbnailStorageKey,
                   CASE
                       WHEN e.event_status = 'ENDED' OR e.event_end_at < NOW() THEN 'EVENT_ENDED'
                       WHEN NOW() < e.sales_start_at                           THEN 'UPCOMING'
                       WHEN NOW() > e.sales_end_at                             THEN 'SALE_ENDED'
                       WHEN COALESCE(t.product_count, 0) > 0 AND t.all_sold_out THEN 'SOLD_OUT'
                       ELSE 'ON_SALE'
                   END                                    AS saleStatus
            FROM expos e
                        LEFT JOIN LATERAL (
                                        SELECT vv2.name
                                        FROM expo_venue_assignments eva
                                        JOIN venue_reservations vr  ON vr.id = eva.venue_reservation_id
                                        JOIN virtual_venues vv2     ON vv2.id = vr.virtual_venue_id
                                        WHERE eva.expo_id = e.id
                                        ORDER BY eva.id
                                        LIMIT 1
                                    ) vv ON TRUE
            LEFT JOIN LATERAL (
                SELECT ei.file_id
                FROM expo_images ei
                WHERE ei.expo_id = e.id AND ei.image_type = 'THUMBNAIL'
                ORDER BY ei.sort_order, ei.id
                LIMIT 1
            ) th ON TRUE
            LEFT JOIN file_metadata fm ON fm.id = th.file_id
            LEFT JOIN (
                SELECT tp.expo_id,
                       MIN(tp.price)                                    AS min_price,
                       SUM(COALESCE(ti.sold_quantity, 0))               AS total_sold,
                       COUNT(*)                                         AS product_count,
                       BOOL_AND(COALESCE(ti.available_quantity, 0) = 0) AS all_sold_out
                FROM ticket_products tp
                LEFT JOIN ticket_inventories ti ON ti.ticket_product_id = tp.id
                WHERE tp.status <> 'CANCELED'
                GROUP BY tp.expo_id
            ) t ON t.expo_id = e.id
            WHERE e.visibility_status = 'PUBLIC'
              AND e.review_status = 'APPROVED'
              AND e.event_status <> 'CANCELED'
              AND (:keyword    IS NULL OR e.title ILIKE '%' || CAST(:keyword AS TEXT) || '%')
              AND (:categoryId IS NULL OR EXISTS (
                       SELECT 1 FROM expo_categories ec
                       WHERE ec.expo_id = e.id AND ec.category_id = :categoryId))
              AND (:regionCode IS NULL OR e.region_code = CAST(:regionCode AS TEXT))
              AND (CAST(:fromDate AS DATE) IS NULL
                   OR (e.event_end_at AT TIME ZONE 'Asia/Seoul')::date >= CAST(:fromDate AS DATE))
              AND (CAST(:toDate AS DATE) IS NULL
                   OR (e.event_start_at AT TIME ZONE 'Asia/Seoul')::date <= CAST(:toDate AS DATE))
              AND (:minPrice IS NULL OR t.min_price >= :minPrice)
              AND (:maxPrice IS NULL OR t.min_price <= :maxPrice)
              AND (
                    :saleStatus IS NULL
                    OR CAST(:saleStatus AS TEXT) = CASE
                        WHEN e.event_status = 'ENDED' OR e.event_end_at < NOW() THEN 'EVENT_ENDED'
                        WHEN NOW() < e.sales_start_at                           THEN 'UPCOMING'
                        WHEN NOW() > e.sales_end_at                             THEN 'SALE_ENDED'
                        WHEN COALESCE(t.product_count, 0) > 0 AND t.all_sold_out THEN 'SOLD_OUT'
                        ELSE 'ON_SALE'
                    END
              )
            ORDER BY
                CASE WHEN CAST(:sort AS TEXT) = 'DEADLINE' THEN e.sales_end_at END ASC NULLS LAST,
                CASE WHEN CAST(:sort AS TEXT) = 'POPULAR'  THEN COALESCE(t.total_sold, 0) END DESC NULLS LAST,
                e.approved_at DESC NULLS LAST,
                e.id DESC
            """,
            countQuery =
                    """
            SELECT COUNT(*)
            FROM expos e
            LEFT JOIN (
                SELECT tp.expo_id,
                       MIN(tp.price)                                    AS min_price,
                       COUNT(*)                                         AS product_count,
                       BOOL_AND(COALESCE(ti.available_quantity, 0) = 0) AS all_sold_out
                FROM ticket_products tp
                LEFT JOIN ticket_inventories ti ON ti.ticket_product_id = tp.id
                WHERE tp.status <> 'CANCELED'
                GROUP BY tp.expo_id
            ) t ON t.expo_id = e.id
            WHERE e.visibility_status = 'PUBLIC'
              AND e.review_status = 'APPROVED'
              AND e.event_status <> 'CANCELED'
              AND (:keyword    IS NULL OR e.title ILIKE '%' || CAST(:keyword AS TEXT) || '%')
              AND (:categoryId IS NULL OR EXISTS (
                       SELECT 1 FROM expo_categories ec
                       WHERE ec.expo_id = e.id AND ec.category_id = :categoryId))
              AND (:regionCode IS NULL OR e.region_code = CAST(:regionCode AS TEXT))
              AND (CAST(:fromDate AS DATE) IS NULL
                   OR (e.event_end_at AT TIME ZONE 'Asia/Seoul')::date >= CAST(:fromDate AS DATE))
              AND (CAST(:toDate AS DATE) IS NULL
                   OR (e.event_start_at AT TIME ZONE 'Asia/Seoul')::date <= CAST(:toDate AS DATE))
              AND (:minPrice IS NULL OR t.min_price >= :minPrice)
              AND (:maxPrice IS NULL OR t.min_price <= :maxPrice)
              AND (
                    :saleStatus IS NULL
                    OR CAST(:saleStatus AS TEXT) = CASE
                        WHEN e.event_status = 'ENDED' OR e.event_end_at < NOW() THEN 'EVENT_ENDED'
                        WHEN NOW() < e.sales_start_at                           THEN 'UPCOMING'
                        WHEN NOW() > e.sales_end_at                             THEN 'SALE_ENDED'
                        WHEN COALESCE(t.product_count, 0) > 0 AND t.all_sold_out THEN 'SOLD_OUT'
                        ELSE 'ON_SALE'
                    END
              )
            """,
            nativeQuery = true)
    // CHECKSTYLE:OFF ParameterNumber
    Page<ExpoCardProjection> searchPublic(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("regionCode") String regionCode,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("saleStatus") String saleStatus,
            @Param("sort") String sort,
            Pageable pageable);
    // CHECKSTYLE:ON ParameterNumber
    // spotless:on

    /** 개최 신청 목록에 "승인으로 만들어진 박람회 ID" 를 붙일 때 쓴다. */
    List<Expo> findByOpeningRequestIdIn(Collection<Long> openingRequestIds);
}
