package com.expo.admin.service;

import com.expo.admin.dto.AdminAuditLogSearchPage;
import com.expo.admin.repository.AdminAuditLogMapper;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 심사·변경·결제·정산 운영 이력 조회 (E-API-018). */
@Service
@Transactional(readOnly = true)
public class AdminAuditLogService {

    /** 한 번에 너무 많이 퍼가지 못하게 막는다. */
    private static final int MAX_PAGE_SIZE = 100;

    private final AdminAuditLogMapper adminAuditLogMapper;

    public AdminAuditLogService(AdminAuditLogMapper adminAuditLogMapper) {
        this.adminAuditLogMapper = adminAuditLogMapper;
    }

    /**
     * 업무 유형(logType)·대상 ID·상태·기간으로 검색한다.
     *
     * <p>{@code offset} 을 {@code long} 으로 계산한다. {@code int} 로 곱하면 {@code page} 가 큰 값일 때
     * 오버플로해 음수 offset 이 되고, PostgreSQL 이 {@code OFFSET must not be negative} 로 거절해 500 이
     * 나간다.
     *
     * @param page 0부터. 음수는 0으로, 데이터 범위를 넘는 값은 빈 목록으로 돌아온다
     * @param size 최대 {@value #MAX_PAGE_SIZE}
     */
    public AdminAuditLogSearchPage searchLogs(
            String logType,
            Long targetId,
            String toStatus,
            Instant startAt,
            Instant endAt,
            int page,
            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        long offset = (long) safePage * safeSize;

        return new AdminAuditLogSearchPage(
                adminAuditLogMapper.countSearch(logType, targetId, toStatus, startAt, endAt),
                safePage,
                safeSize,
                adminAuditLogMapper.search(
                        logType, targetId, toStatus, startAt, endAt, safeSize, offset));
    }
}
