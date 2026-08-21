package com.expo.ticket.repository;

import com.expo.ticket.entity.TicketOrder;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketOrderRepository extends JpaRepository<TicketOrder, Long> {
    Optional<TicketOrder> findByOrderNumber(@Param("orderNumber") String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ticketOrder from TicketOrder ticketOrder where ticketOrder.id = :orderId")
    Optional<TicketOrder> findByIdForUpdate(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select ticketOrder from TicketOrder ticketOrder where ticketOrder.orderNumber = :orderNumber")
    Optional<TicketOrder> findByOrderNumberForUpdate(@Param("orderNumber") String orderNumber);
}
