package com.expo.settlement.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.settlement.dto.AdminSettlementResponse;
import com.expo.settlement.dto.ClientSettlementDetail;
import com.expo.settlement.dto.ClientSettlementResponse;
import com.expo.settlement.dto.SettlementPage;
import com.expo.settlement.repository.SettlementQueryMapper;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정산 조회 (D-API-011 · 012 · 013).
 *
 * <h2>권한 경계가 쿼리 안에 있다</h2>
 *
 * 클라이언트 조회는 {@code hostClientId} 를 <b>조건에 넣어</b> 남의 정산이 결과에 나오지 않게 한다.
 * "찾은 뒤에 주인을 확인" 하는 방식이 아니다 — 확인을 빠뜨릴 자리를 만들지 않는 편이 안전하고,
 * 남의 정산 ID 를 넣어도 <b>"없음" 과 구분되지 않아</b> 존재 여부조차 흘리지 않는다.
 *
 * <h2>관리자와 클라이언트는 보는 것이 다르다</h2>
 *
 * <pre>
 * 클라이언트  금액 구성 전체 + 리포트 + 송금 결과.  자기 것만
 * 관리자      업체명 + 확정자 + 기한.               전체, 검색 가능
 * </pre>
 *
 * 뷰 둘이 이 차이를 이미 담고 있어 여기서 컬럼을 고르지 않는다.
 */
@Service
public class SettlementQueryService {

    /** 한 번에 가져갈 수 있는 최대 건수. */
    private static final int MAX_PAGE_SIZE = 100;

    private final SettlementQueryMapper queryMapper;

    public SettlementQueryService(SettlementQueryMapper queryMapper) {
        this.queryMapper = queryMapper;
    }

    /** D-API-011. 내 정산 목록. 기한이 임박한 것부터. */
    @Transactional(readOnly = true)
    public SettlementPage<ClientSettlementResponse> findMySettlements(
            Long hostClientId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        // long 으로 계산한다. int 로 곱하면 큰 페이지에서 오버플로해 음수 offset 이 된다.
        long offset = (long) safePage * safeSize;

        return new SettlementPage<>(
                queryMapper.countClientSettlements(hostClientId),
                safePage,
                safeSize,
                queryMapper.findClientSettlements(hostClientId, safeSize, offset));
    }

    /**
     * D-API-012. 내 정산 상세 + 금액 구성 항목.
     *
     * <p>남의 정산 ID 를 넣으면 {@code SETTLEMENT_NOT_FOUND} 다. <b>403 이 아니라 404 인 것이
     * 의도다</b> — 403 을 주면 "그 ID 의 정산이 존재하기는 한다" 는 사실이 새어 나간다.
     */
    @Transactional(readOnly = true)
    public ClientSettlementDetail findMySettlement(Long settlementId, Long hostClientId) {
        ClientSettlementResponse settlement =
                queryMapper.findClientSettlement(settlementId, hostClientId);
        if (settlement == null) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND);
        }
        return new ClientSettlementDetail(
                settlement, queryMapper.findSettlementItems(settlementId));
    }

    /** D-API-013. 관리자 목록·검색. 조건은 전부 선택이다. */
    @Transactional(readOnly = true)
    public SettlementPage<AdminSettlementResponse> searchSettlements(
            String status, Long hostClientId, Long expoId, Instant dueBefore, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        long offset = (long) safePage * safeSize;

        return new SettlementPage<>(
                queryMapper.countAdminSettlements(status, hostClientId, expoId, dueBefore),
                safePage,
                safeSize,
                queryMapper.findAdminSettlements(
                        status, hostClientId, expoId, dueBefore, safeSize, offset));
    }
}
