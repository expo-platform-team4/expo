package com.expo.settlement.repository;

import com.expo.settlement.dto.AdminSettlementResponse;
import com.expo.settlement.dto.ClientSettlementResponse;
import com.expo.settlement.dto.SettlementItemResponse;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 정산 조회 (D-API-011 · 012 · 013).
 *
 * <p>클라이언트 조회는 <b>모든 메서드가 {@code hostClientId} 를 받는다.</b> 조회 뒤에 주인을 확인하는
 * 방식이 아니라 <b>조건에 넣어</b> 남의 정산이 결과에 나오지 않게 한다 — 확인을 빠뜨릴 자리를
 * 만들지 않는 편이 안전하다.
 */
@Mapper
public interface SettlementQueryMapper {

    List<ClientSettlementResponse> findClientSettlements(
            @Param("hostClientId") Long hostClientId,
            @Param("size") int size,
            @Param("offset") long offset);

    long countClientSettlements(@Param("hostClientId") Long hostClientId);

    /** 내 정산 하나. 남의 것이면 {@code null} 이다. */
    ClientSettlementResponse findClientSettlement(
            @Param("settlementId") Long settlementId, @Param("hostClientId") Long hostClientId);

    /** 정산 금액의 구성 항목. 티켓·부스·수수료·조정이 종류로 갈린다. */
    List<SettlementItemResponse> findSettlementItems(@Param("settlementId") Long settlementId);

    List<AdminSettlementResponse> findAdminSettlements(
            @Param("status") String status,
            @Param("hostClientId") Long hostClientId,
            @Param("expoId") Long expoId,
            @Param("dueBefore") Instant dueBefore,
            @Param("size") int size,
            @Param("offset") long offset);

    long countAdminSettlements(
            @Param("status") String status,
            @Param("hostClientId") Long hostClientId,
            @Param("expoId") Long expoId,
            @Param("dueBefore") Instant dueBefore);
}
