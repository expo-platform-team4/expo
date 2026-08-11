package com.expo.booth.repository;

import com.expo.booth.entity.BoothAllocation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 부스 확정 배정 영속성 접근 인터페이스. */
public interface BoothAllocationRepository extends JpaRepository<BoothAllocation, Long> {

    Optional<BoothAllocation> findByIdAndClientUserId(Long id, Long clientUserId);

    Optional<BoothAllocation> findByApplicationId(Long applicationId);
}
