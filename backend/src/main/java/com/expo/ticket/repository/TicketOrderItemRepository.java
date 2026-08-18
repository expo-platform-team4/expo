package com.expo.ticket.repository;

import com.expo.ticket.entity.TicketOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketOrderItemRepository extends JpaRepository<TicketOrderItem, Long> {}
