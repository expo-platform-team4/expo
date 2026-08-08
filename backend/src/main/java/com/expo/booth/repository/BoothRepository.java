package com.expo.booth.repository;

import com.expo.booth.entity.Booth;
import org.springframework.data.jpa.repository.JpaRepository;

/** 구역 도면에 존재하는 고정 부스 공간 영속성 접근 인터페이스. */
public interface BoothRepository extends JpaRepository<Booth, Long> {

    boolean existsByVenueZoneIdAndBoothNumber(Long venueZoneId, String boothNumber);
}
