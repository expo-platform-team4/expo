package com.expo.venue.repository;

import com.expo.venue.entity.VenueReservationHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 장소 예약 확정·해제 이력 영속성 접근 인터페이스. */
public interface VenueReservationHistoryRepository
        extends JpaRepository<VenueReservationHistory, Long> {

    List<VenueReservationHistory> findAllByVenueReservationIdOrderByCreatedAtDescIdDesc(
            Long venueReservationId);
}
