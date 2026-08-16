package com.expo.ticket.repository;

import com.expo.ticket.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {}
