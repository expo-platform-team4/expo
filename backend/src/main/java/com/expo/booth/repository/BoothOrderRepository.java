package com.expo.booth.repository;

import com.expo.booth.entity.BoothOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 부스 상품 주문 영속성 접근 인터페이스. */
public interface BoothOrderRepository extends JpaRepository<BoothOrder, Long> {

    Optional<BoothOrder> findByIdAndClientUserId(Long id, Long clientUserId);
}
