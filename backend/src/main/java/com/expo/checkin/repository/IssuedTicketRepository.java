package com.expo.checkin.repository;

import com.expo.checkin.entity.IssuedTicket;
import org.springframework.data.jpa.repository.JpaRepository;

/** 발권 티켓 영속성 접근 인터페이스. */
public interface IssuedTicketRepository extends JpaRepository<IssuedTicket, Long> {}
