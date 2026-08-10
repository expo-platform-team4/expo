package com.expo.venue.repository;

import com.expo.venue.entity.VenueReservation;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 장소 예약 영속성 접근 인터페이스. */
public interface VenueReservationRepository extends JpaRepository<VenueReservation, Long> {

    /**
     * 같은 장소·홀·구역에 기간이 겹치는 확정 예약이 있는지 확인한다.
     *
     * <p>DB의 {@code ex_venue_reservations_period} EXCLUDE 제약과 동일하게 홀·구역이 NULL 일 수 있어
     * COALESCE 로 -1 을 넣어 비교한다.
     */
    @Query(
            "SELECT COUNT(r) > 0 FROM VenueReservation r "
                    + "WHERE r.status = com.expo.venue.entity.VenueReservationStatus.CONFIRMED "
                    + "AND r.virtualVenueId = :virtualVenueId "
                    + "AND COALESCE(r.venueHallId, -1) = COALESCE(:venueHallId, -1) "
                    + "AND COALESCE(r.venueZoneId, -1) = COALESCE(:venueZoneId, -1) "
                    + "AND r.useStartAt < :useEndAt AND r.useEndAt > :useStartAt")
    boolean existsOverlapping(
            @Param("virtualVenueId") Long virtualVenueId,
            @Param("venueHallId") Long venueHallId,
            @Param("venueZoneId") Long venueZoneId,
            @Param("useStartAt") LocalDateTime useStartAt,
            @Param("useEndAt") LocalDateTime useEndAt);
}
