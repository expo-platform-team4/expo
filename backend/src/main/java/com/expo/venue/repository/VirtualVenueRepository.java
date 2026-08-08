package com.expo.venue.repository;

import com.expo.venue.entity.VirtualVenue;
import org.springframework.data.jpa.repository.JpaRepository;

/** 플랫폼이 보유한 행사장·전시장 원본 영속성 접근 인터페이스. */
public interface VirtualVenueRepository extends JpaRepository<VirtualVenue, Long> {}
