package com.expo.booth.repository;

import com.expo.booth.entity.BoothAllocation;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 부스 확정 배정 영속성 접근 인터페이스. */
public interface BoothAllocationRepository extends JpaRepository<BoothAllocation, Long> {

    Optional<BoothAllocation> findByIdAndClientUserId(Long id, Long clientUserId);

    Optional<BoothAllocation> findByApplicationId(Long applicationId);

    /** 상태를 읽고 그 결과로 취소하는 처리 앞에서 행 잠금을 걸어 동시 취소를 막는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM BoothAllocation a WHERE a.id = :id")
    Optional<BoothAllocation> findByIdForUpdate(@Param("id") Long id);
}
