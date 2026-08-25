package com.expo.banner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expo.banner.entity.BannerApplication.ReviewStatus;
import com.expo.banner.service.BannerApplicationService;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.support.PostgresContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주최사가 자기 배너 신청을 취소하는 경로를 본다.
 *
 * <h2>여기서 잡는 것</h2>
 *
 * 취소는 <b>경로에 신청 ID 를 직접 받는</b> 첫 클라이언트 엔드포인트다. 목록 조회는
 * {@code clientUserId} 로 걸러 읽어서 남의 것이 섞일 수 없었지만, 취소는 로그인만 되어 있으면
 * <b>남의 신청 번호를 넣어 부를 수 있다.</b> 소유권 확인이 실제로 막는지가 핵심이다.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
@Transactional
class BannerApplicationCancelIntegrationTest {

    @Autowired private BannerApplicationService applicationService;
    @Autowired private JdbcTemplate jdbc;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;

    private Long ownerId;
    private Long strangerId;
    private Long expoId;
    private Long imageFileId;

    @BeforeEach
    void seed() {
        ownerId = insertClient("owner-cancel@espotic.com");
        strangerId = insertClient("stranger-cancel@espotic.com");
        expoId = insertExpo(ownerId);
        imageFileId = insertImageFile();
    }

    /** 본인의 심사 대기 신청은 취소된다. */
    @Test
    void ownerCancelsOwnPendingApplication() {
        Long applicationId = insertApplication(ownerId, ReviewStatus.UNDER_REVIEW);

        applicationService.cancelApplication(ownerId, applicationId);

        assertThat(statusOf(applicationId)).isEqualTo("CANCELED");
    }

    /**
     * <b>남의 신청은 취소되지 않는다.</b> 이 테스트가 이 기능에서 가장 중요한 한 줄이다.
     *
     * <p>막지 않으면 로그인한 아무나 남의 광고를 내릴 수 있다.
     */
    @Test
    void strangerCannotCancelSomeoneElsesApplication() {
        Long applicationId = insertApplication(ownerId, ReviewStatus.UNDER_REVIEW);

        assertThatThrownBy(() -> applicationService.cancelApplication(strangerId, applicationId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);

        assertThat(statusOf(applicationId)).isEqualTo("UNDER_REVIEW"); // 그대로다
    }

    /**
     * 승인된 신청은 취소되지 않는다.
     *
     * <p>승인은 이미 노출 배너를 만들어 슬롯 자리를 차지했다. 신청서만 취소하면 <b>배너는 계속
     * 노출되는데 신청서는 취소됨</b> 인 어긋난 상태가 된다.
     */
    @Test
    void approvedApplicationCannotBeCanceled() {
        Long applicationId = insertApplication(ownerId, ReviewStatus.APPROVED);

        assertThatThrownBy(() -> applicationService.cancelApplication(ownerId, applicationId))
                .isInstanceOf(com.expo.banner.exception.BannerStateException.class);

        assertThat(statusOf(applicationId)).isEqualTo("APPROVED");
    }

    /**
     * 취소 이력이 남는다. 처리자는 <b>주최사 본인</b>이다.
     *
     * <p>이력 테이블의 컬럼 이름은 {@code reviewer_admin_id} 지만 담기는 것은 "누가 했나" 다.
     * 취소는 관리자가 아니라 주최사가 하고, 그 시점에 {@code reviewed_by_admin_id} 는 아직 없다 —
     * 관리자 ID 를 넣으려 했다면 NOT NULL 위반으로 터졌을 자리다.
     */
    @Test
    void recordsCancelHistoryWithTheClientAsActor() {
        Long applicationId = insertApplication(ownerId, ReviewStatus.UNDER_REVIEW);

        applicationService.cancelApplication(ownerId, applicationId);
        em.flush();

        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT reviewer_admin_id FROM banner_review_histories
                                 WHERE banner_application_id = ? AND decision = 'CANCEL'
                                """,
                                Long.class,
                                applicationId))
                .isEqualTo(ownerId);
    }

    // ------------------------------------------------------------------

    private String statusOf(Long applicationId) {
        em.flush();
        return jdbc.queryForObject(
                "SELECT review_status FROM banner_applications WHERE id = ?",
                String.class,
                applicationId);
    }

    /** 닉네임도 UNIQUE 다. 둘을 만드는 테스트라 이메일에서 뽑아 서로 다르게 준다. */
    private Long insertClient(String email) {
        Long userId =
                jdbc.queryForObject(
                        """
                        INSERT INTO users (email, nickname, role, account_status)
                        VALUES (?, ?, 'CLIENT', 'ACTIVE')
                        RETURNING id
                        """,
                        Long.class,
                        email,
                        email.substring(0, email.indexOf('@')));
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
                VALUES (?, '취소 검증 박람회', 'x', '11',
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
                VALUES ('S3', 'expo-local', 'banner/cancel-test.png',
                        'cancel-test.png', 'image/png', 1024, 'ACTIVE')
                RETURNING id
                """,
                Long.class);
    }

    private Long insertApplication(Long clientUserId, ReviewStatus status) {
        return jdbc.queryForObject(
                """
                INSERT INTO banner_applications (client_user_id, expo_id, image_file_id,
                       headline, requested_start_at, requested_end_at, review_status, submitted_at)
                VALUES (?, ?, ?, '취소 검증',
                        now() + interval '3 day', now() + interval '10 day', ?, now())
                RETURNING id
                """,
                Long.class,
                clientUserId,
                expoId,
                imageFileId,
                status.name());
    }
}
