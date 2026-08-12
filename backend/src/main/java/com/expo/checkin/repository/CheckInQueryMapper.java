package com.expo.checkin.repository;

import com.expo.checkin.dto.CheckInHistoryRow;
import com.expo.checkin.dto.CheckInSummaryResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 체크인 현황·이력 조회.
 *
 * <p>집계와 목록이라 JPA 보다 SQL 이 낫다. 쓰기는 {@link IssuedTicketRepository}·{@link CheckInHistoryRepository}
 * 가 맡고 여기서는 읽기만 한다.
 */
@Mapper
public interface CheckInQueryMapper {

    /** 박람회 하나의 발권·입장·미입장·취소 집계. 쿼리 한 번으로 센다. */
    CheckInSummaryResponse summarize(@Param("expoId") Long expoId);

    /**
     * 체크인 이력을 최신순으로 읽는다.
     *
     * @param limit 한 페이지 크기
     * @param offset 건너뛸 개수. {@code page * size} 가 {@code int} 를 넘길 수 있어 {@code long} 이다
     */
    List<CheckInHistoryRow> findHistory(
            @Param("expoId") Long expoId, @Param("limit") int limit, @Param("offset") long offset);

    /** 이력 전체 건수. 페이지네이션용이다. */
    long countHistory(@Param("expoId") Long expoId);
}
