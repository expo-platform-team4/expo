package com.expo.venue.repository;

import com.expo.venue.entity.VenueHall;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 가상 장소 안의 홀 영속성 접근 인터페이스. */
public interface VenueHallRepository extends JpaRepository<VenueHall, Long> {

    boolean existsByVenueIdAndHallCode(Long venueId, String hallCode);

    boolean existsByIdAndVenueId(Long id, Long venueId);

    long countByVenueId(Long venueId);

    List<VenueHall> findAllByVenueIdOrderByHallCodeAsc(Long venueId);

    Optional<VenueHall> findByIdAndVenueId(Long id, Long venueId);
}
