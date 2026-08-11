package com.expo.ticket.repository;

import com.expo.ticket.entity.TicketProduct;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 티켓 상품 영속성 접근 인터페이스. */
public interface TicketProductRepository extends JpaRepository<TicketProduct, Long> {
    List<TicketProduct> findByExpoId(Long expoId);

    Optional<TicketProduct> findByIdAndExpoId(Long id, Long expoId);
}
