package com.expo.venue.repository;

import com.expo.venue.entity.VenueZone;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 홀 안의 부스 배치 구역 영속성 접근 인터페이스. */
public interface VenueZoneRepository extends JpaRepository<VenueZone, Long> {

    boolean existsByHallIdAndZoneCode(Long hallId, String zoneCode);

    boolean existsByIdAndHallId(Long id, Long hallId);

    long countByHallId(Long hallId);

    List<VenueZone> findAllByHallIdOrderByZoneCodeAsc(Long hallId);

    Optional<VenueZone> findByIdAndHallId(Long id, Long hallId);
}
