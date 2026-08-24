package com.expo.banner.repository;

import com.expo.banner.entity.Banner;
import com.expo.banner.entity.Banner.DisplayStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    /**
     * B-API-024: 현재 노출 가능한 메인 배너 조회 — 슬롯 안에서 ACTIVE 상태인 배너를
     * 노출 순서(sortOrder) 기준으로 정렬한다. 최대 개수는 BannerSlot.maxActiveCount 를
     * 기준으로 호출부에서 Pageable(size=maxActiveCount)로 지정한다.
     */
    @Query(
            """
            select b from Banner b
            where b.bannerSlotId = :slotId
              and b.displayStatus = :status
            order by b.sortOrder asc, b.createdAt asc
            """)
    List<Banner> findActiveBySlot(
            @Param("slotId") Long slotId, @Param("status") DisplayStatus status, Pageable pageable);

    /** 슬롯의 현재 활성 배너 수 (승인 시 sortOrder 산정용) */
    long countByBannerSlotIdAndDisplayStatus(Long bannerSlotId, DisplayStatus displayStatus);

    /**
     * B-API-021: 기간 충돌 조회(상세) — 같은 슬롯에서 SCHEDULED/ACTIVE 상태인 배너 중
     * 주어진 노출 기간과 겹치는 건을 전부 찾는다. 관리자가 드릴다운해서 볼 때 사용.
     */
    @Query(
            """
            select b from Banner b
            where b.bannerSlotId = :slotId
              and b.displayStatus in :statuses
              and b.startAt < :endAt
              and :startAt < b.endAt
            order by b.startAt asc
            """)
    List<Banner> findOverlapping(
            @Param("slotId") Long slotId,
            @Param("statuses") List<DisplayStatus> statuses,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt);

    /**
     * B-API-021: 기간 충돌 조회(목록용) — 관리자 목록 화면에서 신청 건마다 겹치는 배너가
     * 있는지 여부만 가볍게 확인한다.
     */
    @Query(
            """
            select count(b) > 0 from Banner b
            where b.bannerSlotId = :slotId
              and b.displayStatus in :statuses
              and b.startAt < :endAt
              and :startAt < b.endAt
            """)
    boolean existsOverlapping(
            @Param("slotId") Long slotId,
            @Param("statuses") List<DisplayStatus> statuses,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt);
}
