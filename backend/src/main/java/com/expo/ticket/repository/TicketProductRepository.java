package com.expo.ticket.repository;

import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.entity.TicketProductStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 티켓 상품 영속성 접근 인터페이스. */
public interface TicketProductRepository extends JpaRepository<TicketProduct, Long> {
    List<TicketProduct> findByExpoId(Long expoId);

    Optional<TicketProduct> findByIdAndExpoId(Long id, Long expoId);

    @Query(
            """
        SELECT product
            FROM TicketProduct product
            JOIN FETCH product.inventory inventory
            WHERE product.expoId = :expoId
                AND product.status = :status
                AND inventory.totalQuantity
                    > inventory.reservedQuantity + inventory.soldQuantity
    """)
    List<TicketProduct> findPurchasableByExpoId(
            @Param("expoId") Long expoId, @Param("status") TicketProductStatus status);
}
