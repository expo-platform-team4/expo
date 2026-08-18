package com.expo.settlement.repository;

import com.expo.settlement.entity.SettlementItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 정산 항목 영속성 접근 인터페이스. */
public interface SettlementItemRepository extends JpaRepository<SettlementItem, Long> {

    /**
     * 재계산할 때 이전 항목을 지운다. 누적하면 금액이 두 배가 된다.
     *
     * <h2>파생 삭제({@code deleteBySettlementId})를 쓰면 안 된다</h2>
     *
     * 그쪽은 엔티티를 지운 표시만 하고 <b>실제 DELETE 를 flush 까지 미룬다.</b> 그런데 Hibernate 의
     * 액션 큐는 <b>INSERT 를 DELETE 보다 먼저</b> 실행한다. 그래서 "지우고 다시 넣기" 가
     * "넣고 지우기" 가 되어 {@code uq_settlement_items_source} 에 걸린다.
     *
     * <p>실제로 그렇게 짰다가 통합 테스트에서 잡혔다. mock 으로는 순서 자체가 없어 안 나온다.
     *
     * <p>{@code @Modifying} 벌크 삭제는 <b>즉시 실행</b>된다. {@code flushAutomatically} 로 앞선 변경을
     * 먼저 반영하고, {@code clearAutomatically} 로 영속성 컨텍스트에 남은 옛 항목을 비운다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SettlementItem i where i.settlementId = :settlementId")
    void deleteAllBySettlementId(@Param("settlementId") Long settlementId);

    List<SettlementItem> findBySettlementIdOrderByIdAsc(Long settlementId);
}
