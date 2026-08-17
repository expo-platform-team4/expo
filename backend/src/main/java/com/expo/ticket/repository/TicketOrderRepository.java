package com.expo.ticket.repository;

import com.expo.ticket.entity.TicketOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface TicketOrderRepository extends JpaRepository<TicketOrder, Long> {
    Optional<TicketOrder> findByOrderNumber(@Param("orderNumber") String orderNumber);
}
