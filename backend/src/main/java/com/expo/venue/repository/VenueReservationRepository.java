package com.expo.venue.repository;

import com.expo.venue.entity.VenueReservation;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 장소 예약 영속성 접근 인터페이스. */
public interface VenueReservationRepository extends JpaRepository<VenueReservation, Long> {

    /**
     * 같은 장소·홀·구역에 기간이 겹치는 확정 예약이 있는지 확인한다.
     *
     * <p>DB의 {@code ex_venue_reservations_period} EXCLUDE 제약과 동일하게 홀·구역이 NULL 일 수 있어
     * COALESCE 로 -1 을 넣어 비교하되(정확히 같은 계층 겹침), 홀 전체 예약(구역 NULL)과 그 아래 구역 단위
     * 예약처럼 계층이 서로 다른 겹침도 함께 확인한다. 이 계층 간 겹침은 DB의
     * {@code trg_venue_reservation_hierarchy_conflict} 트리거가 최종적으로 막는다.
     */
    @Query(
            "SELECT COUNT(r) > 0 FROM VenueReservation r "
                    + "WHERE r.status = com.expo.venue.entity.VenueReservationStatus.CONFIRMED "
                    + "AND r.virtualVenueId = :virtualVenueId "
                    + "AND r.useStartAt < :useEndAt AND r.useEndAt > :useStartAt "
                    + "AND ("
                    + "  (COALESCE(r.venueHallId, -1) = COALESCE(:venueHallId, -1) "
                    + "    AND COALESCE(r.venueZoneId, -1) = COALESCE(:venueZoneId, -1)) "
                    + "  OR (:venueHallId IS NOT NULL AND :venueZoneId IS NULL "
                    + "    AND r.venueHallId = :venueHallId AND r.venueZoneId IS NOT NULL) "
                    + "  OR (:venueZoneId IS NOT NULL "
                    + "    AND r.venueHallId = :venueHallId AND r.venueZoneId IS NULL)"
                    + ")")
    boolean existsOverlapping(
            @Param("virtualVenueId") Long virtualVenueId,
            @Param("venueHallId") Long venueHallId,
            @Param("venueZoneId") Long venueZoneId,
            @Param("useStartAt") Instant useStartAt,
            @Param("useEndAt") Instant useEndAt);
}
