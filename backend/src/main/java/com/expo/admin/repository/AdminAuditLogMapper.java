package com.expo.admin.repository;

import com.expo.admin.dto.AdminAuditLogResponse;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminAuditLogMapper {

    /**
     * @param logType 업무 유형 필터 (없으면 전체). {@code v_admin_audit_logs.log_type} 값
     * @param targetId 대상 ID 필터 (없으면 전체)
     * @param toStatus 상태 필터 (없으면 전체). {@code to_status} 기준
     * @param startAt 조회 시작 시각 (없으면 전체)
     * @param endAt 조회 종료 시각 (없으면 전체)
     * @param limit 한 페이지 크기
     * @param offset 건너뛸 개수. {@code page * size} 가 {@code int} 를 넘길 수 있어 {@code long} 이다
     */
    List<AdminAuditLogResponse> search(
            @Param("logType") String logType,
            @Param("targetId") Long targetId,
            @Param("toStatus") String toStatus,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("limit") int limit,
            @Param("offset") long offset);

    /** {@link #search} 와 동일한 필터로 전체 건수를 센다. 페이지 계산용. */
    long countSearch(
            @Param("logType") String logType,
            @Param("targetId") Long targetId,
            @Param("toStatus") String toStatus,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt);
}
