package com.expo.venue.repository;

import com.expo.venue.entity.VenueReservation;
import org.springframework.data.jpa.repository.JpaRepository;

/** 장소 예약 영속성 접근 인터페이스. */
public interface VenueReservationRepository extends JpaRepository<VenueReservation, Long> {}
