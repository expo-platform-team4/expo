package com.expo.banner;

import static org.assertj.core.api.Assertions.assertThat;

import com.expo.banner.dto.BannerDisplaySyncResult;
import com.expo.banner.service.BannerApplicationService;
import com.expo.banner.service.BannerDisplayStatusService;
import com.expo.support.PostgresContainerConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배너가 <b>시각에 맞춰 켜지고 꺼지는지</b> 본다.
 *
 * <h2>여기서 잡는 결함</h2>
 *
 * 승인은 시작일이 미래면 배너를 {@code SCHEDULED} 로 만든다. 그런데 그것을 {@code ACTIVE} 로
 * 바꿔 주는 코드가 <b>저장소 어디에도 없었다.</b> 엔티티에 {@code activate()} 는 있고 주석도
 * "스케줄러가 호출" 이라고 적혀 있었지만 부르는 쪽이 없었다.
 *
 * <p>증상이 조용하다 — 승인은 성공하고 화면에도 "승인됨" 으로 보인다. <b>시간이 지나도 안 뜨는
 * 것을 사람이 알아채야만</b> 드러난다.
 *
 * <h2>왜 실제 DB 인가</h2>
 *
 * 검증 대상이 <b>시각 비교와 상태 전이의 조합</b>이다. mock 으로는 조회 조건이 맞는지 알 수 없고,
 * 공개 조회가 실제로 그 배너를 집어 오는지는 더더욱 확인되지 않는다.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
@Transactional
class BannerDisplayStatusIntegrationTest {

    @Autowired private BannerDisplayStatusService displayStatusService;
    @Autowired private BannerApplicationService applicationService;
    @Autowired private JdbcTemplate jdbc;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;

    private Long slotId;
    private Long expoId;
    private Long clientUserId;
    private Long imageFileId;

    @BeforeEach
    void seed() {
        clientUserId = insertClient();
        expoId = insertExpo(clientUserId);
        slotId = ensureMainSlot();
        imageFileId = insertImageFile();
        jdbc.update("DELETE FROM banners");
    }

    /**
     * 배너의 현재 상태를 <b>DB 에서</b> 읽는다.
     *
     * <p>먼저 {@code flush()} 한다. 서비스가 바꾼 엔티티는 영속성 컨텍스트에만 있고, 이 테스트는
     * 같은 트랜잭션이라 raw SQL 로 읽으면 <b>바뀌기 전 값</b>이 보인다. 운영에서는 트랜잭션이
     * 커밋되므로 생기지 않는 일이고, 테스트가 중간에 끼어들어 보기 때문에 필요한 처리다.
     */
    private String statusOf(Long bannerId) {
        em.flush();
        return jdbc.queryForObject(
                "SELECT display_status FROM banners WHERE id = ?", String.class, bannerId);
    }

    private List<Long> activeBannerIds() {
        return applicationService.getActiveBanners().stream()
                .map(com.expo.banner.dto.ActiveBannerResponse::id)
                .toList();
    }

    // ------------------------------------------------------------------

    /**
     * <b>시작일이 된 예약 배너가 켜진다.</b>
     *
     * <p>이 테스트가 이 기능의 존재 이유다. 정리를 부르기 전에는 공개 조회에 안 나오고, 부른 뒤에
     * 나와야 한다.
     */
    @Test
    void activatesScheduledBannerWhoseStartTimeHasPassed() {
        Long bannerId = insertBanner("SCHEDULED", "-1 hour", "+7 day", 0);

        assertThat(activeBannerIds()).doesNotContain(bannerId);

        BannerDisplaySyncResult result = displayStatusService.sync();

        assertThat(result.activatedCount()).isEqualTo(1);
        assertThat(statusOf(bannerId)).isEqualTo("ACTIVE");
        assertThat(activeBannerIds()).contains(bannerId);
    }

    /** 아직 시작 전인 예약 배너는 그대로 둔다. */
    @Test
    void leavesScheduledBannerBeforeItsStartTime() {
        Long bannerId = insertBanner("SCHEDULED", "+1 day", "+7 day", 0);

        assertThat(displayStatusService.sync().activatedCount()).isZero();
        assertThat(statusOf(bannerId)).isEqualTo("SCHEDULED");
    }

    /** 종료일이 지난 노출 배너는 끝난다. */
    @Test
    void endsActiveBannerWhoseEndTimeHasPassed() {
        Long bannerId = insertBanner("ACTIVE", "-30 day", "-1 hour", 0);

        BannerDisplaySyncResult result = displayStatusService.sync();

        assertThat(result.endedCount()).isEqualTo(1);
        assertThat(statusOf(bannerId)).isEqualTo("ENDED");
        assertThat(activeBannerIds()).doesNotContain(bannerId);
    }

    /**
     * 켜 보지도 못하고 기간이 지난 배너도 끝난다.
     *
     * <p>끝내지 않으면 <b>슬롯 정원을 계속 차지한다</b> — 노출된 적도 없으면서 뒤에 대기 중인
     * 배너의 자리를 막는다.
     */
    @Test
    void endsScheduledBannerThatExpiredWithoutEverShowing() {
        Long bannerId = insertBanner("SCHEDULED", "-30 day", "-1 hour", 0);

        BannerDisplaySyncResult result = displayStatusService.sync();

        assertThat(result.endedCount()).isEqualTo(1);
        assertThat(result.activatedCount()).isZero(); // 켰다 끄는 왕복을 하지 않는다
        assertThat(statusOf(bannerId)).isEqualTo("ENDED");
    }

    /** 두 번 불러도 아무 일이 없다. 조회 조건이 곧 "아직 처리되지 않은 것" 이다. */
    @Test
    void secondSyncDoesNothing() {
        insertBanner("SCHEDULED", "-1 hour", "+7 day", 0);
        insertBanner("ACTIVE", "-30 day", "-1 hour", 1);

        displayStatusService.sync();
        BannerDisplaySyncResult second = displayStatusService.sync();

        assertThat(second.activatedCount()).isZero();
        assertThat(second.endedCount()).isZero();
    }

    /**
     * <b>끝난 배너가 비워진 뒤에 새 배너가 켜진다.</b>
     *
     * <p>순서가 결과를 바꾼다. 켜기를 먼저 하면 끝난 배너가 정원을 차지한 상태에서 새 배너가
     * 올라와 그 순간 정원을 넘길 수 있다. 슬롯 정원이 1일 때 그 차이가 드러난다.
     */
    @Test
    void endsBeforeActivatingSoCapacityIsCorrect() {
        jdbc.update("UPDATE banner_slots SET max_active_count = 1 WHERE id = ?", slotId);
        Long expiring = insertBanner("ACTIVE", "-30 day", "-1 hour", 0);
        Long incoming = insertBanner("SCHEDULED", "-1 hour", "+7 day", 1);

        displayStatusService.sync();

        assertThat(statusOf(expiring)).isEqualTo("ENDED");
        assertThat(statusOf(incoming)).isEqualTo("ACTIVE");
        // 정원 1을 새 배너가 차지한다. 끝난 배너가 자리를 물고 있지 않다
        assertThat(activeBannerIds()).containsExactly(incoming);
    }

    /** 이미 끝났거나 취소된 배너는 건드리지 않는다. */
    @Test
    void ignoresAlreadyFinishedBanners() {
        Long ended = insertBanner("ENDED", "-30 day", "-1 hour", 0);
        Long canceled = insertBanner("CANCELED", "-30 day", "+7 day", 1);

        BannerDisplaySyncResult result = displayStatusService.sync();

        assertThat(result.endedCount()).isZero();
        assertThat(result.activatedCount()).isZero();
        assertThat(statusOf(ended)).isEqualTo("ENDED");
        assertThat(statusOf(canceled)).isEqualTo("CANCELED");
    }

    // ------------------------------------------------------------------
    // 시드
    // ------------------------------------------------------------------

    private Long insertClient() {
        Long userId =
                jdbc.queryForObject(
                        """
                        INSERT INTO users (email, nickname, role, account_status)
                        VALUES ('host-banner@espotic.com', '주최사', 'CLIENT', 'ACTIVE')
                        RETURNING id
                        """,
                        Long.class);
        jdbc.update(
                """
                INSERT INTO client_profiles (user_id, company_name, business_number,
                       representative_name, business_address)
                VALUES (?, '주최사', ?, '대표', '서울')
                """,
                userId,
                "%010d".formatted(userId));
        return userId;
    }

    private Long insertExpo(Long hostClientId) {
        return jdbc.queryForObject(
                """
                INSERT INTO expos (host_client_id, title, description, region_code,
                       event_start_at, event_end_at, sales_start_at, sales_end_at,
                       review_status, visibility_status, event_status)
                VALUES (?, '배너 검증 박람회', 'x', '11',
                        now() + interval '10 day', now() + interval '12 day',
                        now() - interval '30 day', now() + interval '9 day',
                        'APPROVED', 'PUBLIC', 'SCHEDULED')
                RETURNING id
                """,
                Long.class,
                hostClientId);
    }

    /** 메인 슬롯. 마이그레이션이 이미 넣어 두었으면 그것을 쓴다. */
    private Long ensureMainSlot() {
        List<Long> existing =
                jdbc.queryForList("SELECT id FROM banner_slots ORDER BY id LIMIT 1", Long.class);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return jdbc.queryForObject(
                """
                INSERT INTO banner_slots (slot_code, name, max_active_count,
                       width_px, height_px, active)
                VALUES ('MAIN', '메인 배너', 5, 1200, 400, true)
                RETURNING id
                """,
                Long.class);
    }

    /** 배너 이미지. {@code banners.image_file_id} 가 NOT NULL 이라 없으면 배너를 못 만든다. */
    private Long insertImageFile() {
        return jdbc.queryForObject(
                """
                INSERT INTO file_metadata (storage_provider, bucket_name, storage_key,
                       original_filename, content_type, file_size, file_status)
                VALUES ('S3', 'expo-local', 'banner/test.png',
                        'test.png', 'image/png', 1024, 'ACTIVE')
                RETURNING id
                """,
                Long.class);
    }

    /**
     * 배너 한 건.
     *
     * <p>배너는 <b>신청 없이 존재할 수 없다</b>({@code banner_application_id} 가 NOT NULL). 승인된
     * 신청에서 만들어지는 것이 정상 경로이므로, 시드도 신청을 먼저 만들고 그것을 가리킨다.
     *
     * @param startOffset {@code now()} 기준 간격. 예: {@code "-1 hour"}
     */
    private Long insertBanner(
            String displayStatus, String startOffset, String endOffset, int sortOrder) {
        Long applicationId =
                jdbc.queryForObject(
                        """
                        INSERT INTO banner_applications (client_user_id, expo_id, image_file_id,
                               headline, requested_start_at, requested_end_at, review_status)
                        VALUES (?, ?, ?, '배너 문구',
                                now() + interval '%s', now() + interval '%s', 'APPROVED')
                        RETURNING id
                        """
                                .formatted(startOffset, endOffset),
                        Long.class,
                        clientUserId,
                        expoId,
                        imageFileId);

        return jdbc.queryForObject(
                """
                INSERT INTO banners (banner_application_id, banner_slot_id, expo_id, image_file_id,
                       headline, start_at, end_at, display_status, sort_order)
                VALUES (?, ?, ?, ?, '배너 문구',
                        now() + interval '%s', now() + interval '%s', ?, ?)
                RETURNING id
                """
                        .formatted(startOffset, endOffset),
                Long.class,
                applicationId,
                slotId,
                expoId,
                imageFileId,
                displayStatus,
                sortOrder);
    }
}
