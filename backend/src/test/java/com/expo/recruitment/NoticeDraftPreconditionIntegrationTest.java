package com.expo.recruitment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expo.common.exception.BusinessException;
import com.expo.recruitment.dto.CreateRecruitmentNoticeRequest;
import com.expo.recruitment.dto.RecruitmentNoticeRequestResponse;
import com.expo.recruitment.service.RecruitmentNoticeRequestService;
import com.expo.recruitment.service.RecruitmentNoticeService;
import com.expo.support.PostgresContainerConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 요청 목록이 말하는 <b>"초안을 만들 수 있다"</b> 와 실제 생성 결과가 일치하는지 본다.
 *
 * <h2>여기서 잡는 결함</h2>
 *
 * 초안 생성 화면은 {@code status == APPROVED} 인 요청만 골라 보여줬다. 그런데 그 상태는
 * <b>장소 판정의 부산물</b>이다 — 관리자가 장소를 허용하면 그 자리에서 APPROVED 가 된다
 * ({@code RecruitmentNoticeRequest#decideVenue}). 장소 예약은 <b>별개 버튼</b>으로 따로
 * 확정해야 하는데, 그 사이에 초안 생성을 누르면 서버가 거절했다.
 *
 * <p>화면은 "고를 수 있다" 고 하고 서버는 "안 된다" 고 하는 상태였다. 두 API 를 각각 테스트하면
 * 둘 다 자기 몫은 맞게 하므로 <b>사이가 안 보인다.</b>
 *
 * <h2>그래서 무엇을 검증하나</h2>
 *
 * 플래그 값 자체가 아니라 <b>플래그와 실제 결과가 같은 말을 하는지</b> 본다. 조건이 나중에 하나
 * 늘어도 한쪽만 고치면 이 테스트가 깨진다.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
@Transactional
class NoticeDraftPreconditionIntegrationTest {

    @Autowired private RecruitmentNoticeRequestService requestService;
    @Autowired private RecruitmentNoticeService noticeService;
    @Autowired private JdbcTemplate jdbc;

    private Long hostClientId;
    private Long adminId;
    private Long venueId;
    private Long hallId;
    private Long zoneId;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM recruitment_notice_histories");
        jdbc.update("DELETE FROM venue_reservation_histories");
        jdbc.update("DELETE FROM venue_reservations");
        jdbc.update("DELETE FROM recruitment_notices");
        jdbc.update("DELETE FROM recruitment_notice_request_zones");
        jdbc.update("DELETE FROM recruitment_notice_request_histories");
        jdbc.update("DELETE FROM recruitment_notice_requests");

        hostClientId = insertClient("host-draft@espotic.com", "host-draft");
        adminId = insertUser("admin-draft@espotic.com", "admin-draft", "ADMIN");
        venueId = ensureVirtualVenue();
        hallId = ensureHall(venueId);
        zoneId = ensureZone(hallId);
    }

    /** 장소 판정 전에는 만들 수 없다고 말한다. */
    @Test
    void reportsNotCreatableBeforeVenueDecision() {
        Long requestId = insertRequest("SUBMITTED", "PENDING");

        RecruitmentNoticeRequestResponse response = findInAdminList(requestId);

        assertThat(response.venueReservationConfirmed()).isFalse();
        assertThat(response.noticeCreated()).isFalse();
    }

    /**
     * <b>장소만 허용하고 예약을 안 잡았으면 만들 수 없다고 말한다.</b>
     *
     * <p>이 상태가 이 테스트의 이유다. 예전에는 여기서 화면이 "고를 수 있다" 고 했다 —
     * {@code status} 가 이미 APPROVED 이기 때문이다.
     */
    @Test
    void reportsNotCreatableWhenVenueAllowedButNotReserved() {
        Long requestId = insertRequest("APPROVED", "ALLOWED");

        RecruitmentNoticeRequestResponse response = findInAdminList(requestId);

        assertThat(response.venueDecision().name()).isEqualTo("ALLOWED");
        assertThat(response.status().name()).isEqualTo("APPROVED"); // 예전에 이것만 봤다
        assertThat(response.venueReservationConfirmed()).isFalse();
    }

    /** 예약이 확정되면 만들 수 있다고 말한다. */
    @Test
    void reportsCreatableOnceReservationIsConfirmed() {
        Long requestId = insertRequest("APPROVED", "ALLOWED");
        insertReservation(requestId, "CONFIRMED");

        RecruitmentNoticeRequestResponse response = findInAdminList(requestId);

        assertThat(response.venueReservationConfirmed()).isTrue();
        assertThat(response.noticeCreated()).isFalse();
    }

    /** 해제된 예약이 섞여 있으면 만들 수 없다고 말한다. 그 구역은 더 이상 우리 것이 아니다. */
    @Test
    void reportsNotCreatableWhenAnyReservationIsReleased() {
        Long requestId = insertRequest("APPROVED", "ALLOWED");
        insertReservation(requestId, "CONFIRMED");
        insertReservation(requestId, "RELEASED");

        assertThat(findInAdminList(requestId).venueReservationConfirmed()).isFalse();
    }

    /** 공고가 이미 만들어졌으면 그렇다고 말한다. 요청 하나당 공고는 하나다. */
    @Test
    void reportsNoticeAlreadyCreated() {
        Long requestId = insertRequest("APPROVED", "ALLOWED");
        insertReservation(requestId, "CONFIRMED");
        noticeService.create(adminId, createRequestFor(requestId));

        RecruitmentNoticeRequestResponse response = findInAdminList(requestId);

        assertThat(response.noticeCreated()).isTrue();
    }

    /**
     * <b>목록이 말하는 것과 실제 생성 결과가 같아야 한다.</b>
     *
     * <p>이 테스트가 이 클래스의 핵심이다. 위의 개별 검증은 플래그 값이 맞는지만 보지만, 여기서는
     * <b>플래그대로 실제로 되는지</b>를 확인한다. 조건이 나중에 하나 늘었는데 한쪽만 고치면
     * 여기서 깨진다 — 화면과 서버가 갈라지는 것을 막는 자리다.
     */
    @Test
    void listAgreesWithWhatCreateActuallyDoes() {
        Long beforeDecision = insertRequest("SUBMITTED", "PENDING");
        Long allowedNoReservation = insertRequest("APPROVED", "ALLOWED");
        Long ready = insertRequest("APPROVED", "ALLOWED");
        insertReservation(ready, "CONFIRMED");

        for (Long requestId : List.of(beforeDecision, allowedNoReservation, ready)) {
            RecruitmentNoticeRequestResponse row = findInAdminList(requestId);
            boolean listSaysCreatable =
                    row.venueDecision().name().equals("ALLOWED")
                            && row.venueReservationConfirmed()
                            && !row.noticeCreated();

            if (listSaysCreatable) {
                assertThat(noticeService.create(adminId, createRequestFor(requestId))).isNotNull();
            } else {
                assertThatThrownBy(() -> noticeService.create(adminId, createRequestFor(requestId)))
                        .as("목록은 못 만든다고 했는데 생성이 성공했다. requestId=%d", requestId)
                        .isInstanceOf(BusinessException.class);
            }
        }
    }

    // ------------------------------------------------------------------

    private RecruitmentNoticeRequestResponse findInAdminList(Long requestId) {
        return requestService.listForAdmin().stream()
                .filter(r -> r.id().equals(requestId))
                .findFirst()
                .orElseThrow();
    }

    /** 신청 마감은 장소 사용 시작일보다 앞서야 한다 — 예약을 만들 때 쓰는 시각과 맞춰 둔다. */
    private CreateRecruitmentNoticeRequest createRequestFor(Long requestId) {
        return new CreateRecruitmentNoticeRequest(
                requestId,
                "초안 제목",
                "초안 내용",
                null,
                null,
                Instant.now().plus(1, ChronoUnit.DAYS),
                Instant.now().plus(5, ChronoUnit.DAYS));
    }

    private Long insertUser(String email, String nickname, String role) {
        return jdbc.queryForObject(
                """
                INSERT INTO users (email, nickname, role, account_status)
                VALUES (?, ?, ?, 'ACTIVE')
                RETURNING id
                """,
                Long.class,
                email,
                nickname,
                role);
    }

    /**
     * 주최 클라이언트. {@code users} 행만으로는 부족하다 — {@code recruitment_notice_requests.host_client_id}
     * 의 FK 가 {@code users} 가 아니라 <b>{@code client_profiles}</b> 를 가리킨다.
     */
    private Long insertClient(String email, String nickname) {
        Long userId = insertUser(email, nickname, "CLIENT");
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

    /**
     * 장소·홀·구역은 <b>새로 만들지 않고 있는 것을 쓴다.</b>
     *
     * <p>세 테이블 모두 개수 상한을 <b>DB 트리거</b>로 막고 있다(장소 총 개수, 장소당 홀 2개,
     * 홀당 구역 5개). 테스트가 매번 새로 만들면 상한에 걸려 터진다 — 실제로 그렇게 터졌다.
     * 마이그레이션이 넣어 둔 것을 쓰고, 없을 때만 만든다.
     */
    private Long ensureVirtualVenue() {
        return firstIdOr(
                "SELECT id FROM virtual_venues ORDER BY id LIMIT 1",
                () ->
                        jdbc.queryForObject(
                                """
                                INSERT INTO virtual_venues (name, address, region_code)
                                VALUES ('초안 검증 장소', '서울', '11')
                                RETURNING id
                                """,
                                Long.class));
    }

    private Long ensureHall(Long virtualVenueId) {
        return firstIdOr(
                "SELECT id FROM venue_halls WHERE venue_id = %d ORDER BY id LIMIT 1"
                        .formatted(virtualVenueId),
                () ->
                        jdbc.queryForObject(
                                """
                                INSERT INTO venue_halls (venue_id, name, hall_code)
                                VALUES (?, '초안 검증 홀', 'H-DRAFT')
                                RETURNING id
                                """,
                                Long.class,
                                virtualVenueId));
    }

    private Long ensureZone(Long venueHallId) {
        return firstIdOr(
                "SELECT id FROM venue_zones WHERE hall_id = %d ORDER BY id LIMIT 1"
                        .formatted(venueHallId),
                () ->
                        jdbc.queryForObject(
                                """
                                INSERT INTO venue_zones (hall_id, name, zone_code, max_booth_count)
                                VALUES (?, '초안 검증 구역', 'Z-DRAFT', 10)
                                RETURNING id
                                """,
                                Long.class,
                                venueHallId));
    }

    private Long firstIdOr(String selectSql, java.util.function.Supplier<Long> create) {
        List<Long> existing = jdbc.queryForList(selectSql, Long.class);
        return existing.isEmpty() ? create.get() : existing.get(0);
    }

    private Long insertRequest(String status, String venueDecision) {
        Long requestId =
                jdbc.queryForObject(
                        """
                        INSERT INTO recruitment_notice_requests
                          (host_client_id, title, description,
                           application_start_at, application_end_at,
                           event_start_at, event_end_at,
                           virtual_venue_id, venue_hall_id, target_company_count,
                           status, venue_conflict_status, venue_decision, submitted_at)
                        VALUES (?, '초안 검증 요청', '설명',
                                now() + interval '1 day', now() + interval '5 day',
                                now() + interval '30 day', now() + interval '32 day',
                                ?, ?, 10, ?, 'CLEAR', ?, now())
                        RETURNING id
                        """,
                        Long.class,
                        hostClientId,
                        venueId,
                        hallId,
                        status,
                        venueDecision);
        jdbc.update(
                """
                INSERT INTO recruitment_notice_request_zones (request_id, venue_hall_id, venue_zone_id)
                VALUES (?, ?, ?)
                """,
                requestId,
                hallId,
                zoneId);
        return requestId;
    }

    /**
     * 장소 예약 한 건.
     *
     * <p>사용 시작일을 신청 마감(+5일)보다 뒤로 둔다. 앞서면 {@code create()} 가 기간 검증에서
     * 막혀, 이 테스트가 보려는 <b>예약 유무</b>가 아니라 다른 이유로 실패한다.
     */
    private void insertReservation(Long requestId, String status) {
        jdbc.update(
                """
                INSERT INTO venue_reservations
                  (reservation_source_type, notice_request_id, virtual_venue_id, venue_hall_id,
                   venue_zone_id, use_start_at, use_end_at, status, confirmed_by_admin_id, confirmed_at)
                VALUES ('RECRUITMENT_NOTICE', ?, ?, ?, ?,
                        now() + interval '30 day', now() + interval '32 day', ?, ?, now())
                """,
                requestId,
                venueId,
                hallId,
                zoneId,
                status,
                adminId);
    }
}
