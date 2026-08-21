package com.expo.ticket.repository;

import com.expo.ticket.entity.InventoryReservation;
import com.expo.ticket.entity.InventoryReservationStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    List<InventoryReservation> findAllByTicketOrderId(Long ticketOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
        select reservation
          from InventoryReservation reservation
         where reservation.ticketOrder.id = :ticketOrderId
        """)
    List<InventoryReservation> findAllByTicketOrderIdForUpdate(
            @Param("ticketOrderId") Long ticketOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
        select reservation
          from InventoryReservation reservation
         where reservation.status = :status
           and reservation.expiresAt <= :now
        """)
    List<InventoryReservation> findAllExpiredForUpdate(
            @Param("status") InventoryReservationStatus status, @Param("now") Instant now);
}
