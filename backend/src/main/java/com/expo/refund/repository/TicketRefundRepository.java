package com.expo.refund.repository;

import com.expo.refund.entity.TicketRefund;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 티켓 주문 환불 요청 영속성 접근 인터페이스. */
public interface TicketRefundRepository extends JpaRepository<TicketRefund, Long> {

    Optional<TicketRefund> findByTicketOrderId(Long ticketOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select refund from TicketRefund refund where refund.id = :refundId")
    Optional<TicketRefund> findByIdForUpdate(@Param("refundId") Long refundId);
}
