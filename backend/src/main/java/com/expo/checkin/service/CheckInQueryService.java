package com.expo.checkin.service;

import com.expo.checkin.dto.CheckInHistoryPage;
import com.expo.checkin.dto.CheckInSummaryResponse;
import com.expo.checkin.repository.CheckInQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주최사의 체크인 현황·이력 조회.
 *
 * <p>입장 처리({@link CheckInService})와 서비스를 나눈 이유는 성격이 달라서다. 처리는 쓰기이고 행 잠금과 격리 수준을 신경 써야 하지만, 이쪽은
 * 순수 읽기라 {@code readOnly} 로 둘 수 있다.
 */
@Service
public class CheckInQueryService {

    /** 한 번에 너무 많이 퍼가지 못하게 막는다. */
    private static final int MAX_PAGE_SIZE = 100;

    private final CheckInQueryMapper checkInQueryMapper;
    private final ExpoHostVerifier expoHostVerifier;

    public CheckInQueryService(
            CheckInQueryMapper checkInQueryMapper, ExpoHostVerifier expoHostVerifier) {
        this.checkInQueryMapper = checkInQueryMapper;
        this.expoHostVerifier = expoHostVerifier;
    }

    /** 발권·입장·미입장·취소 집계. */
    @Transactional(readOnly = true)
    public CheckInSummaryResponse summary(Long expoId, Long clientUserId) {
        expoHostVerifier.verifyHost(expoId, clientUserId);
        return checkInQueryMapper.summarize(expoId);
    }

    /**
     * 체크인 이력. 최신순이다.
     *
     * @param page 0부터
     * @param size 최대 {@value #MAX_PAGE_SIZE}
     */
    @Transactional(readOnly = true)
    public CheckInHistoryPage history(Long expoId, Long clientUserId, int page, int size) {
        expoHostVerifier.verifyHost(expoId, clientUserId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);

        return new CheckInHistoryPage(
                checkInQueryMapper.countHistory(expoId),
                safePage,
                safeSize,
                checkInQueryMapper.findHistory(expoId, safeSize, safePage * safeSize));
    }
}
