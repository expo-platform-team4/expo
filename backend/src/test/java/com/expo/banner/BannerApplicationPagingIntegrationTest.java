package com.expo.banner;

import static org.assertj.core.api.Assertions.assertThat;

import com.expo.banner.dto.BannerApplicationAdminResponse;
import com.expo.banner.dto.BannerApplicationResponse;
import com.expo.banner.service.BannerApplicationService;
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
 * 목록을 페이지로 나눠 읽어도 <b>빠지거나 겹치는 신청이 없는지</b> 본다.
 *
 * <h2>여기서 잡는 결함</h2>
 *
 * 관리자 목록은 {@code createdAt} 하나로만 정렬했고 내 신청 목록은 <b>정렬이 아예 없었다.</b>
 * LIMIT/OFFSET 은 정해진 순서 위에서만 의미가 있어서, 순서가 흔들리면 <b>어떤 신청은 두 페이지에
 * 다 나오고 어떤 신청은 영영 안 나온다.</b>
 *
 * <p>실제로 그랬다 — 로컬 DB 에서 2페이지를 열었더니 1페이지의 신청이 다시 보였다.
 * 정렬이 없어도 <b>한 페이지만 보면 멀쩡해 보인다</b>는 것이 이 결함의 성질이다.
 *
 * <h2>재현 조건 둘</h2>
 *
 * <b>하나. 같은 시각.</b> 시드가 {@code createdAt} 을 일부러 전부 같게 넣는다. 시각이 조금씩
 * 다르면 그것만으로 순서가 정해져 결함이 있어도 우연히 통과한다.
 *
 * <p><b>둘. 페이지를 넘기는 사이에 행을 바꾼다.</b> 이게 없으면 <b>깨진 코드에서도 테스트가
 * 통과한다</b> — 실제로 처음엔 그랬다. 작은 테이블은 정렬을 안 줘도 매번 같은 순서로 읽히기
 * 때문이다. PostgreSQL 에서 UPDATE 는 행을 <b>새 자리에 다시 쓴다.</b> 그래서 갱신된 행은
 * 읽는 순서의 끝으로 밀리고, 정렬이 없으면 페이지 경계가 그만큼 어긋난다.
 *
 * <p>꾸며낸 상황이 아니다. 관리자가 목록을 넘기는 동안 다른 신청이 승인되거나 취소되는 것은
 * <b>평범한 일이다.</b>
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
@Transactional
class BannerApplicationPagingIntegrationTest {

    private static final int TOTAL = 7;
    private static final int PAGE_SIZE = 2;

    @Autowired private BannerApplicationService applicationService;
    @Autowired private JdbcTemplate jdbc;

    private Long clientUserId;

    @BeforeEach
    void seed() {
        clientUserId = insertClient();
        Long expoId = insertExpo(clientUserId);
        Long imageFileId = insertImageFile();
        jdbc.update("DELETE FROM banner_review_histories");
        jdbc.update("DELETE FROM banners");
        jdbc.update("DELETE FROM banner_applications");
        for (int i = 0; i < TOTAL; i++) {
            insertApplication(expoId, imageFileId, i);
        }
    }

    /** <b>관리자 목록</b>을 끝까지 넘겨 읽으면 전부 한 번씩만 나온다. */
    @Test
    void adminPagesCoverEveryApplicationExactlyOnce() {
        List<Long> collected =
                readAllPages(
                        page ->
                                applicationService
                                        .getApplicationsForAdmin(null, page, PAGE_SIZE)
                                        .getContent()
                                        .stream()
                                        .map(BannerApplicationAdminResponse::id)
                                        .toList());

        assertThat(collected).hasSize(TOTAL).doesNotHaveDuplicates();
    }

    /** <b>내 신청 목록</b>도 마찬가지다. 이쪽은 정렬이 아예 없었다. */
    @Test
    void myPagesCoverEveryApplicationExactlyOnce() {
        List<Long> collected =
                readAllPages(
                        page ->
                                applicationService
                                        .getMyApplications(clientUserId, page, PAGE_SIZE)
                                        .getContent()
                                        .stream()
                                        .map(BannerApplicationResponse::id)
                                        .toList());

        assertThat(collected).hasSize(TOTAL).doesNotHaveDuplicates();
    }

    /** 내 신청 목록은 최근 것이 먼저다. */
    @Test
    void myListShowsNewestFirst() {
        List<Long> firstPage =
                applicationService.getMyApplications(clientUserId, 1, TOTAL).getContent().stream()
                        .map(BannerApplicationResponse::id)
                        .toList();

        assertThat(firstPage).isSortedAccordingTo((a, b) -> Long.compare(b, a));
    }

    /**
     * {@code page} 는 1부터 센다.
     *
     * <p>0을 보내면 1페이지와 같은 결과가 온다 — 에러가 아니라 <b>조용히 같은 값</b>이다.
     * 부르는 쪽이 0부터 센다고 착각하면 마지막 페이지 하나를 통째로 놓친다.
     */
    @Test
    void pageNumberingStartsAtOne() {
        List<Long> pageZero = idsOfAdminPage(0);
        List<Long> pageOne = idsOfAdminPage(1);
        List<Long> pageTwo = idsOfAdminPage(2);

        assertThat(pageZero).isEqualTo(pageOne);
        assertThat(pageTwo).doesNotContainAnyElementsOf(pageOne);
    }

    // ------------------------------------------------------------------

    private List<Long> idsOfAdminPage(int page) {
        return applicationService
                .getApplicationsForAdmin(null, page, PAGE_SIZE)
                .getContent()
                .stream()
                .map(BannerApplicationAdminResponse::id)
                .toList();
    }

    /**
     * 1페이지부터 빈 페이지가 나올 때까지 읽어 모은다.
     *
     * <p>매 페이지 사이에 <b>이미 읽은 행 하나를 건드린다.</b> 위 주석 참고 — 이게 없으면 정렬이
     * 없어도 테스트가 통과해 버린다.
     */
    private List<Long> readAllPages(java.util.function.IntFunction<List<Long>> pageReader) {
        List<Long> collected = new java.util.ArrayList<>();
        for (int page = 1; page <= TOTAL; page++) {
            List<Long> ids = pageReader.apply(page);
            if (ids.isEmpty()) {
                break;
            }
            collected.addAll(ids);
            touch(ids.get(0));
        }
        return collected;
    }

    /**
     * 행을 갱신해 <b>읽히는 순서를 흔든다.</b>
     *
     * <p>{@code createdAt} 은 건드리지 않는다 — 정렬 키를 바꿔 버리면 순서가 달라지는 게 당연해져
     * 검증이 무의미해진다. 바꾸는 것은 정렬과 무관한 컬럼뿐이고, 그런데도 순서가 흔들린다는 점이
     * 요지다.
     */
    private void touch(Long applicationId) {
        jdbc.update(
                "UPDATE banner_applications SET updated_at = now() WHERE id = ?", applicationId);
    }

    private Long insertClient() {
        Long userId =
                jdbc.queryForObject(
                        """
                        INSERT INTO users (email, nickname, role, account_status)
                        VALUES ('host-paging@espotic.com', 'host-paging', 'CLIENT', 'ACTIVE')
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
                VALUES (?, '페이징 검증 박람회', 'x', '11',
                        now() + interval '10 day', now() + interval '12 day',
                        now() - interval '30 day', now() + interval '9 day',
                        'APPROVED', 'PUBLIC', 'SCHEDULED')
                RETURNING id
                """,
                Long.class,
                hostClientId);
    }

    private Long insertImageFile() {
        return jdbc.queryForObject(
                """
                INSERT INTO file_metadata (storage_provider, bucket_name, storage_key,
                       original_filename, content_type, file_size, file_status)
                VALUES ('S3', 'expo-local', 'banner/paging-test.png',
                        'paging-test.png', 'image/png', 1024, 'ACTIVE')
                RETURNING id
                """,
                Long.class);
    }

    /**
     * 신청 한 건. {@code created_at} 을 <b>일부러 고정</b>해 동점을 만든다 — 정렬 결함을 재현하는
     * 조건이 그것이다.
     */
    private void insertApplication(Long expoId, Long imageFileId, int index) {
        jdbc.update(
                """
                INSERT INTO banner_applications (client_user_id, expo_id, image_file_id,
                       headline, requested_start_at, requested_end_at, review_status,
                       submitted_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, now() + interval '3 day', now() + interval '10 day',
                        'UNDER_REVIEW', TIMESTAMPTZ '2026-08-01 00:00:00+09',
                        TIMESTAMPTZ '2026-08-01 00:00:00+09', TIMESTAMPTZ '2026-08-01 00:00:00+09')
                """,
                clientUserId,
                expoId,
                imageFileId,
                "페이징 검증 " + index);
    }
}
