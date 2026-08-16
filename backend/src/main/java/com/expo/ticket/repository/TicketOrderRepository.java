package com.expo.ticket.repository;

import com.expo.ticket.entity.TicketOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketOrderRepository extends JpaRepository<TicketOrder, Long> {}
