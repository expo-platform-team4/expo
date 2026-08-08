package com.expo.venue.repository;

import com.expo.venue.entity.VirtualVenue;
import org.springframework.data.jpa.repository.JpaRepository;

/** 가상 장소 영속성 접근 인터페이스. */
public interface VirtualVenueRepository extends JpaRepository<VirtualVenue, Long> {

    boolean existsByName(String name);
}
