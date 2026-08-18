package com.expo.settlement.repository;

import com.expo.settlement.dto.SettlementTarget;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 정산 대상 박람회를 찾는다 (D-API-017). */
@Mapper
public interface SettlementGenerationMapper {

    /**
     * 정산을 만들어야 할 박람회들. 종료가 이른 것부터 나온다.
     *
     * <p>박람회 도메인을 읽지만 <b>엔티티에 의존하지 않는다.</b> {@code Expo} 엔티티가 아직 없기도 하고,
     * 여기서 필요한 건 네 필드뿐이다.
     *
     * @param endedBefore 이 시각 이전에 끝난 박람회. 호출부가 "지금 - 7일" 을 넘긴다
     */
    List<SettlementTarget> findDueExpos(@Param("endedBefore") Instant endedBefore);
}
