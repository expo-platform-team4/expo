package com.expo.banner.service;

import com.expo.banner.dto.BannerDisplaySyncResult;
import com.expo.banner.entity.Banner;
import com.expo.banner.repository.BannerRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배너의 노출 상태를 시각에 맞춘다.
 *
 * <h2>왜 필요한가</h2>
 *
 * 승인(B-API-022)은 시작일이 미래면 배너를 {@code SCHEDULED} 로 만들어 둔다. 엔티티에
 * {@link Banner#activate} 가 있고 주석도 <i>"스케줄러가 start_at 도달 시 호출"</i> 이라고 적혀
 * 있었지만, <b>부르는 코드가 저장소 어디에도 없었다.</b>
 *
 * <p>그래서 이런 상태였다.
 *
 * <pre>
 * 미래 시작으로 승인   → SCHEDULED
 * 시작일 도달          → 아무도 ACTIVE 로 바꾸지 않는다
 * 공개 조회            → ACTIVE 만 보므로 영원히 안 뜬다
 * 종료일 도달          → ENDED 로도 안 바뀌어 슬롯 정원을 계속 차지한다
 * </pre>
 *
 * <b>예약 배너가 영원히 노출되지 않는다.</b> 승인은 성공하고 화면에도 "승인됨" 으로 보이므로
 * 아무 에러가 없다 — 시간이 지나도 안 뜨는 것을 사람이 알아채야만 드러난다.
 *
 * <h2>끄기를 먼저 한다</h2>
 *
 * 순서가 결과를 바꾼다. 켜기를 먼저 하면 <b>끝난 배너가 정원을 차지한 상태에서</b> 새 배너가
 * 올라와, 그 순간 정원을 넘길 수 있다. 끝난 것을 먼저 비우고 켜야 자리 계산이 맞는다.
 *
 * <h2>여러 번 불러도 안전하다</h2>
 *
 * 조회 조건이 곧 "아직 처리되지 않은 것" 이다. 두 번째 호출은 대상이 0건이라 아무 일도 하지 않는다.
 * {@code BannerDisplayStatusScheduler} 가 5분마다 부르고, 급할 때 사람이 내부 API 로 불러도 된다 —
 * 둘이 겹쳐도 같은 성질이 유지된다.
 */
@Slf4j
@Service
public class BannerDisplayStatusService {

    private final BannerRepository bannerRepository;

    public BannerDisplayStatusService(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    /**
     * 시각이 지난 배너의 상태를 정리한다.
     *
     * <p>한 트랜잭션에서 끝낸다. 배너 수가 슬롯 정원 규모라 나눌 이유가 없고, 끄기와 켜기가 같은
     * 슬롯의 자리를 두고 이어져 있어 <b>중간 상태가 보이지 않는 편</b>이 낫다.
     */
    @Transactional
    public BannerDisplaySyncResult sync() {
        OffsetDateTime now = OffsetDateTime.now();

        // ① 끝난 것을 먼저 비운다. 위 주석 참고 — 순서가 정원 계산을 바꾼다.
        List<Banner> toEnd = bannerRepository.findDueToEnd(now);
        toEnd.forEach(banner -> banner.end(now));

        // ② 그다음에 켠다.
        List<Banner> toActivate = bannerRepository.findDueToActivate(now);
        toActivate.forEach(banner -> banner.activate(now));

        if (!toEnd.isEmpty() || !toActivate.isEmpty()) {
            log.info("배너 노출 상태 정리 종료={}건 활성화={}건", toEnd.size(), toActivate.size());
        }

        return new BannerDisplaySyncResult(
                toActivate.size(),
                toEnd.size(),
                toActivate.stream().map(Banner::getId).toList(),
                toEnd.stream().map(Banner::getId).toList());
    }
}
