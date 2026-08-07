package com.expo.ticket.repository;

import com.expo.ticket.entity.TicketInventory;
import org.springframework.data.jpa.repository.JpaRepository;

/** 티켓 재고 영속성 접근 인터페이스. */
public interface TicketInventoryRepository extends JpaRepository<TicketInventory, Long> {}
