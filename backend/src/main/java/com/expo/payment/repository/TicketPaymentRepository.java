package com.expo.payment.repository;

import com.expo.payment.entity.TicketPayment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketPaymentRepository extends JpaRepository<TicketPayment, Long> {

    Optional<TicketPayment> findByTicketOrderId(Long ticketOrderId);
}
