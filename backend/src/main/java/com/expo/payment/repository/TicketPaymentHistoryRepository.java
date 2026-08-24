package com.expo.payment.repository;

import com.expo.payment.dto.AdminTicketPaymentHistoryResponse;
import com.expo.payment.entity.TicketPaymentEventType;
import com.expo.payment.entity.TicketPaymentHistory;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 티켓 결제 감사 이력 영속성 접근 인터페이스. */
public interface TicketPaymentHistoryRepository extends JpaRepository<TicketPaymentHistory, Long> {
    @Query(
            """
        select new com.expo.payment.dto.AdminTicketPaymentHistoryResponse(
            h.id, o.id, o.orderNumber, p.id, h.eventType, h.fromStatus, h.toStatus,
            h.amount, h.pgTransactionKey, h.occurredAt)
          from TicketPaymentHistory h
          join TicketPayment p on p.id = h.ticketPaymentId
          join TicketOrder o on o.id = p.ticketOrderId
         where (:orderNumber is null or o.orderNumber like concat('%', :orderNumber, '%'))
           and (:eventType is null or h.eventType = :eventType)
         order by h.occurredAt desc, h.id desc
        """)
    List<AdminTicketPaymentHistoryResponse> searchAdminHistories(
            @Param("orderNumber") String orderNumber,
            @Param("eventType") TicketPaymentEventType eventType,
            Pageable pageable);
}
