package com.expo.booth.repository;

import com.expo.booth.entity.BoothPayment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 부스 상품 결제 영속성 접근 인터페이스. */
public interface BoothPaymentRepository extends JpaRepository<BoothPayment, Long> {

    List<BoothPayment> findAllByBoothOrderId(Long boothOrderId);

    Optional<BoothPayment> findByPgOrderId(String pgOrderId);
}
