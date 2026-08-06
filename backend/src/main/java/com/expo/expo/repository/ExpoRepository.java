package com.expo.expo.repository;

import com.expo.expo.domain.Expo;
import com.expo.expo.domain.ExpoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ExpoRepository — 박람회 JPA Repository
 *
 * 검색(희-SRCH-01 ~ 13)은 TICKET_TYPE 집계(최저가·매진 여부)와
 * 판매 상태 파생 계산이 필요하므로 PostgreSQL 네이티브 쿼리로 구현.
 */
public interface ExpoRepository extends JpaRepository<Expo, Long> {

    /* ==================== 단건/기본 조회 (논리 삭제 제외, 비고 6) ==================== */

    Optional<Expo> findByExpoIdAndIsDeletedFalse(Long expoId);

    /** 클라이언트별 등록 박람회 목록 (idx_expo_client) */
    Page<Expo> findByClientIdAndIsDeletedFalse(Long clientId, Pageable pageable);

    /** 상태별 목록 — 관리자 심사 대기열 등 (idx_expo_status) */
    Page<Expo> findByStatusAndIsDeletedFalse(ExpoStatus status, Pageable pageable);

    /* ==================== 공개 목록 검색 (희-SRCH-01 ~ 13) ==================== */

    /**
     * 승인(PUBLISHED)된 박람회 목록 검색 — 승인 즉시 자동 목록 반영(희-SRCH-12)
     *
     * 필터: 제목(01) · 카테고리(02) · 지역(03) · 기간(04) · 가격(05) · 판매상태(06)
     * 정렬: LATEST 최신순(07) · DEADLINE 마감임박순(08) · POPULAR 인기순(09)
     * 페이지네이션: Pageable 페이지 번호 방식(10)
     *
     * sale_status 파생 계산(희-EXPO-10, 정의서 비고 4):
     *   행사 종료일 경과        → EVENT_ENDED(행사종료)
     *   티켓 미등록(판매 개시 전) → UPCOMING(판매예정)   … 희-SRCH-13
     *   전 티켓 매진            → SOLD_OUT(매진)
     *   그 외                   → ON_SALE(판매중)
     */
    @Query(value = """
            SELECT e.expo_id                              AS expoId,
                   e.title                                AS title,
                   e.start_date                           AS startDate,
                   e.end_date                             AS endDate,
                   e.region                               AS region,
                   COALESCE(v.name, e.desired_venue)      AS venueName,
                   e.thumbnail_url                        AS thumbnailUrl,
                   t.min_price                            AS minPrice,
                   CASE
                       WHEN e.end_date < CURRENT_DATE            THEN 'EVENT_ENDED'
                       WHEN COALESCE(t.type_count, 0) = 0        THEN 'UPCOMING'
                       WHEN t.all_sold_out                       THEN 'SOLD_OUT'
                       ELSE 'ON_SALE'
                   END                                    AS saleStatus
            FROM expo e
            LEFT JOIN venue v ON v.venue_id = e.venue_id
            LEFT JOIN (
                SELECT tt.expo_id,
                       MIN(tt.price)                                   AS min_price,
                       SUM(tt.sold_quantity)                           AS total_sold,
                       COUNT(*)                                        AS type_count,
                       BOOL_AND(tt.sold_quantity >= tt.total_quantity) AS all_sold_out
                FROM ticket_type tt
                GROUP BY tt.expo_id
            ) t ON t.expo_id = e.expo_id
            WHERE e.is_deleted = FALSE
              AND e.status = 'PUBLISHED'
              AND (:keyword    IS NULL OR e.title ILIKE '%' || CAST(:keyword AS TEXT) || '%')
              AND (:categoryId IS NULL OR e.category_id = :categoryId)
              AND (:region     IS NULL OR e.region = CAST(:region AS TEXT))
              AND (CAST(:fromDate AS DATE) IS NULL OR e.end_date   >= CAST(:fromDate AS DATE))
              AND (CAST(:toDate   AS DATE) IS NULL OR e.start_date <= CAST(:toDate   AS DATE))
              AND (:minPrice IS NULL OR t.min_price >= :minPrice)
              AND (:maxPrice IS NULL OR t.min_price <= :maxPrice)
              AND (
                    :saleStatus IS NULL
                    OR CAST(:saleStatus AS TEXT) = CASE
                        WHEN e.end_date < CURRENT_DATE     THEN 'EVENT_ENDED'
                        WHEN COALESCE(t.type_count, 0) = 0 THEN 'UPCOMING'
                        WHEN t.all_sold_out                THEN 'SOLD_OUT'
                        ELSE 'ON_SALE'
                    END
              )
            ORDER BY
                CASE WHEN CAST(:sort AS TEXT) = 'DEADLINE' THEN e.end_date END ASC NULLS LAST,
                CASE WHEN CAST(:sort AS TEXT) = 'POPULAR'  THEN COALESCE(t.total_sold, 0) END DESC NULLS LAST,
                e.published_at DESC NULLS LAST,
                e.expo_id DESC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM expo e
            LEFT JOIN (
                SELECT tt.expo_id,
                       MIN(tt.price)                                   AS min_price,
                       COUNT(*)                                        AS type_count,
                       BOOL_AND(tt.sold_quantity >= tt.total_quantity) AS all_sold_out
                FROM ticket_type tt
                GROUP BY tt.expo_id
            ) t ON t.expo_id = e.expo_id
            WHERE e.is_deleted = FALSE
              AND e.status = 'PUBLISHED'
              AND (:keyword    IS NULL OR e.title ILIKE '%' || CAST(:keyword AS TEXT) || '%')
              AND (:categoryId IS NULL OR e.category_id = :categoryId)
              AND (:region     IS NULL OR e.region = CAST(:region AS TEXT))
              AND (CAST(:fromDate AS DATE) IS NULL OR e.end_date   >= CAST(:fromDate AS DATE))
              AND (CAST(:toDate   AS DATE) IS NULL OR e.start_date <= CAST(:toDate   AS DATE))
              AND (:minPrice IS NULL OR t.min_price >= :minPrice)
              AND (:maxPrice IS NULL OR t.min_price <= :maxPrice)
              AND (
                    :saleStatus IS NULL
                    OR CAST(:saleStatus AS TEXT) = CASE
                        WHEN e.end_date < CURRENT_DATE     THEN 'EVENT_ENDED'
                        WHEN COALESCE(t.type_count, 0) = 0 THEN 'UPCOMING'
                        WHEN t.all_sold_out                THEN 'SOLD_OUT'
                        ELSE 'ON_SALE'
                    END
              )
            """,
        nativeQuery = true)
    Page<ExpoCardProjection> searchPublished(@Param("keyword") String keyword,
                                             @Param("categoryId") Long categoryId,
                                             @Param("region") String region,
                                             @Param("fromDate") LocalDate fromDate,
                                             @Param("toDate") LocalDate toDate,
                                             @Param("minPrice") Integer minPrice,
                                             @Param("maxPrice") Integer maxPrice,
                                             @Param("saleStatus") String saleStatus,
                                             @Param("sort") String sort,
                                             Pageable pageable);

    /* ==================== 배치/스케줄러용 ==================== */

    /** 행사 종료일이 지난 공개 박람회 조회 (CLOSED 전환 배치용) */
    List<Expo> findByStatusAndEndDateBeforeAndIsDeletedFalse(ExpoStatus status, LocalDate date);
}
