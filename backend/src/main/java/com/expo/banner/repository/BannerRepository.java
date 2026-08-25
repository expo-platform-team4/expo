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

    /**
     * 시작 시각이 지났는데 아직 {@code SCHEDULED} 로 남아 있는 배너.
     *
     * <p>승인 시점에 시작일이 미래면 {@code SCHEDULED} 로 만들어 두는데, <b>그것을 켜 주는 주체가
     * 없었다.</b> 엔티티에 {@code activate()} 는 있고 주석도 "스케줄러가 호출" 이라고 적혀 있었지만
     * 부르는 코드가 저장소 어디에도 없어서, 예약된 배너가 영원히 노출되지 않았다.
     *
     * <p>{@code end_at} 도 함께 본다. 예약해 둔 사이 종료일까지 지나 버린 배너를 켰다가 바로 끄는
     * 왕복을 피한다 — 그런 건 {@link #findDueToEnd} 가 한 번에 끝낸다.
     */
    @Query(
            """
        SELECT banner
            FROM Banner banner
            WHERE banner.displayStatus = com.expo.banner.entity.Banner.DisplayStatus.SCHEDULED
                AND banner.startAt <= :now
                AND banner.endAt   >  :now
            ORDER BY banner.id
    """)
    List<Banner> findDueToActivate(@Param("now") OffsetDateTime now);

    /**
     * 종료 시각이 지났는데 아직 끝나지 않은 배너.
     *
     * <p>{@code SCHEDULED} 도 포함한다. 켜 보지도 못하고 기간이 지난 배너가 있을 수 있고, 그것도
     * 끝난 것으로 정리해야 <b>슬롯 정원을 계속 차지하지 않는다.</b>
     */
    @Query(
            """
        SELECT banner
            FROM Banner banner
            WHERE banner.displayStatus IN (
                    com.expo.banner.entity.Banner.DisplayStatus.SCHEDULED,
                    com.expo.banner.entity.Banner.DisplayStatus.ACTIVE)
                AND banner.endAt <= :now
            ORDER BY banner.id
    """)
    List<Banner> findDueToEnd(@Param("now") OffsetDateTime now);
}
