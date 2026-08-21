package com.expo.booth.repository;

import com.expo.booth.entity.BoothOrder;
import com.expo.booth.entity.BoothOrderStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 부스 상품 주문 영속성 접근 인터페이스. */
public interface BoothOrderRepository extends JpaRepository<BoothOrder, Long> {

    Optional<BoothOrder> findByIdAndClientUserId(Long id, Long clientUserId);

    /** 결제 시도 생성·승인처럼 읽고 그 결과로 분기하는 처리 앞에서 행 잠금을 걸어 동시 처리를 막는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM BoothOrder o WHERE o.id = :id")
    Optional<BoothOrder> findByIdForUpdate(@Param("id") Long id);

    /** 만료 시각이 지났는데 아직 정리되지 않은 주문. 만료 일괄 처리 대상 조회용. */
    List<BoothOrder> findAllByStatusAndExpiresAtBefore(BoothOrderStatus status, Instant expiresAt);
}
