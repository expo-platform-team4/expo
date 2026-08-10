package com.expo.booth.repository;

import com.expo.booth.entity.BoothReservation;
import com.expo.booth.entity.BoothReservationStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 부스 상품 임시 확보 영속성 접근 인터페이스. */
public interface BoothReservationRepository extends JpaRepository<BoothReservation, Long> {

    Optional<BoothReservation> findFirstByBoothOrderIdAndStatus(
            Long boothOrderId, BoothReservationStatus status);
}
