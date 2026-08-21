package com.expo.payment.repository;

import com.expo.payment.entity.TicketPaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/** 티켓 결제 감사 이력 영속성 접근 인터페이스. */
public interface TicketPaymentHistoryRepository extends JpaRepository<TicketPaymentHistory, Long> {}
