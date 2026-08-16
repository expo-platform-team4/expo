package com.expo.ticket.repository;

import com.expo.ticket.entity.TicketInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 티켓 재고 영속성 접근 인터페이스. */
public interface TicketInventoryRepository extends JpaRepository<TicketInventory, Long> {
    @Modifying(clearAutomatically = true)
    @Query(
            """
        UPDATE TicketInventory i
           SET i.reservedQuantity = i.reservedQuantity + :quantity
         WHERE i.ticketProduct.id = :productId
           AND i.totalQuantity - i.reservedQuantity - i.soldQuantity >= :quantity
        """)
    int reserveIfAvailable(@Param("productId") Long productId, @Param("quantity") int quantity);
}
