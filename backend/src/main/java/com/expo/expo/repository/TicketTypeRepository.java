package com.expo.expo.repository;

import com.expo.expo.domain.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    /** 박람회별 티켓 종류 조회 (idx_ticket_type_expo) */
    List<TicketType> findByExpoIdOrderByPriceAsc(Long expoId);
}
