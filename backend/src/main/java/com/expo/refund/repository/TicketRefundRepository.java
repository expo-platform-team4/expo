package com.expo.refund.repository;

import com.expo.refund.entity.TicketRefund;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 티켓 주문 환불 요청 영속성 접근 인터페이스. */
public interface TicketRefundRepository extends JpaRepository<TicketRefund, Long> {

    Optional<TicketRefund> findByTicketOrderId(Long ticketOrderId);
}
