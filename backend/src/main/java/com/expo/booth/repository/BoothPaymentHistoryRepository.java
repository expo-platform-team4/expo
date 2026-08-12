package com.expo.booth.repository;

import com.expo.booth.entity.BoothPaymentHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 부스 상품 결제 이력 영속성 접근 인터페이스. */
public interface BoothPaymentHistoryRepository extends JpaRepository<BoothPaymentHistory, Long> {

    List<BoothPaymentHistory> findAllByBoothPaymentIdOrderByOccurredAtDesc(Long boothPaymentId);
}
