package com.expo.ticket.repository;

import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.entity.TicketProductStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 티켓 상품 영속성 접근 인터페이스. */
public interface TicketProductRepository extends JpaRepository<TicketProduct, Long> {
    List<TicketProduct> findByExpoId(Long expoId);

    Optional<TicketProduct> findByIdAndExpoId(Long id, Long expoId);

    /**
     * 지금 실제로 살 수 있는 티켓 상품.
     *
     * <h2>판매 기간을 함께 본다</h2>
     *
     * {@code status} 컬럼만 보면 안 된다. 판매 기간이 끝나도 <b>그 컬럼을 바꿔 주는 코드가
     * 없어서</b> {@code ON_SALE} 로 남아 있기 때문이다. 그래서 기간이 지난 상품이 "구매 가능"
     * 목록에 그대로 나왔다.
     *
     * <p>증상은 조용했다. 박람회 상세 윗줄은 뷰({@code v_public_expo_cards})가 기간으로 계산해
     * "판매 종료" 를 보여 주는데, 아래 티켓 카드는 이 조회 결과라 "판매중" 이 떴다. <b>같은
     * 화면에서 두 값이 달랐다.</b>
     *
     * <p>주문 단계({@code TicketOrderItemCreatorService})는 기간을 검증하므로 실제로 팔리지는
     * 않았다. 대신 사용자가 "판매중" 을 보고 담았다가 주문에서 막히는 <b>막다른 길</b>이 됐다.
     *
     * @param now 판매 기간 판정 기준 시각. 호출부가 넘긴다 — 쿼리 안에서
     *     {@code CURRENT_TIMESTAMP} 를 쓰면 테스트가 시간을 고정할 수 없다
     */
    @Query(
            """
        SELECT product
            FROM TicketProduct product
            JOIN FETCH product.inventory inventory
            WHERE product.expoId = :expoId
                AND product.status = :status
                AND product.salesStartAt <= :now
                AND product.salesEndAt   >= :now
                AND inventory.totalQuantity
                    > inventory.reservedQuantity + inventory.soldQuantity
    """)
    List<TicketProduct> findPurchasableByExpoId(
            @Param("expoId") Long expoId,
            @Param("status") TicketProductStatus status,
            @Param("now") Instant now);
}
