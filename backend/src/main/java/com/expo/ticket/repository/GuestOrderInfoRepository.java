package com.expo.ticket.repository;

import com.expo.ticket.entity.GuestOrder;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuestOrderInfoRepository extends JpaRepository<GuestOrder, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select guestOrder from GuestOrder guestOrder where guestOrder.ticketOrderId = :ticketOrderId")
    Optional<GuestOrder> findByIdForUpdate(@Param("ticketOrderId") Long ticketOrderId);
}
