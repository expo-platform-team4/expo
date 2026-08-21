package com.expo.ticket.repository;

import com.expo.ticket.entity.InventoryReservation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    List<InventoryReservation> findAllByTicketOrderId(Long ticketOrderId);
}
