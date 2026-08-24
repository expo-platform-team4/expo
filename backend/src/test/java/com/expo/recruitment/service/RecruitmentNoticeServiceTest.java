package com.expo.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.participation.entity.ParticipationApplicationStatus;
import com.expo.participation.repository.ParticipationApplicationRepository;
import com.expo.recruitment.converter.RecruitmentNoticeConverter;
import com.expo.recruitment.dto.CreateRecruitmentNoticeRequest;
import com.expo.recruitment.dto.RecruitmentNoticeResponse;
import com.expo.recruitment.dto.UpdateRecruitmentNoticeRequest;
import com.expo.recruitment.entity.RecruitmentNotice;
import com.expo.recruitment.entity.RecruitmentNoticeActionType;
import com.expo.recruitment.entity.RecruitmentNoticeHistory;
import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import com.expo.recruitment.entity.RecruitmentNoticeStatus;
import com.expo.recruitment.entity.VenueDecision;
import com.expo.recruitment.repository.RecruitmentNoticeHistoryRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRequestRepository;
import com.expo.venue.entity.VenueReservation;
import com.expo.venue.entity.VenueReservationActionType;
import com.expo.venue.entity.VenueReservationHistory;
import com.expo.venue.entity.VenueReservationStatus;
import com.expo.venue.repository.VenueReservationHistoryRepository;
import com.expo.venue.repository.VenueReservationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link RecruitmentNoticeService} 의 승인 상태 검증과 초안·게시·마감 상태 전이 규칙을 검증한다. */
class RecruitmentNoticeServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long REQUEST_ID = 10L;
    private static final Long HOST_CLIENT_ID = 30L;
    private static final Long HALL_ID = 40L;
    private static final Long ZONE_ID = 50L;
    private static final Long OTHER_ZONE_ID = 51L;
    private static final Instant T1 = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-09-30T00:00:00Z");
    private static final Instant T3 = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant T4 = Instant.parse("2026-10-05T00:00:00Z");

    private RecruitmentNoticeRepository recruitmentNoticeRepository;
    private RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository;
    private RecruitmentNoticeHistoryRepository recruitmentNoticeHistoryRepository;
    private ParticipationApplicationRepository participationApplicationRepository;
    private VenueReservationRepository venueReservationRepository;
    private VenueReservationHistoryRepository venueReservationHistoryRepository;
    private RecruitmentNoticeService service;

    @BeforeEach
    void setUp() {
        recruitmentNoticeRepository = mock(RecruitmentNoticeRepository.class);
        recruitmentNoticeRequestRepository = mock(RecruitmentNoticeRequestRepository.class);
        recruitmentNoticeHistoryRepository = mock(RecruitmentNoticeHistoryRepository.class);
        participationApplicationRepository = mock(ParticipationApplicationRepository.class);
        venueReservationRepository = mock(VenueReservationRepository.class);
        venueReservationHistoryRepository = mock(VenueReservationHistoryRepository.class);
        service =
                new RecruitmentNoticeService(
                        recruitmentNoticeRepository,
                        recruitmentNoticeRequestRepository,
                        recruitmentNoticeHistoryRepository,
                        participationApplicationRepository,
                        venueReservationRepository,
                        venueReservationHistoryRepository,
                        new RecruitmentNoticeConverter());
    }

    private CreateRecruitmentNoticeRequest createRequest() {
        return new CreateRecruitmentNoticeRequest(REQUEST_ID, "공고 제목", "공고 내용", null, null, T1, T2);
    }

    private RecruitmentNoticeRequest allowedNoticeRequest() {
        RecruitmentNoticeRequest entity =
                RecruitmentNoticeRequest.create(HOST_CLIENT_ID, "제목", "설명", T1, T2, T3, T4, 1L);
        entity.decideVenue(VenueDecision.ALLOWED, ADMIN_ID, "충돌 없음");
        return entity;
    }

    private VenueReservation confirmedReservation(Long noticeRequestId) {
        return confirmedReservation(noticeRequestId, ZONE_ID);
    }

    private VenueReservation confirmedReservation(Long noticeRequestId, Long zoneId) {
        return VenueReservation.confirmForRecruitmentNotice(
                noticeRequestId, 1L, HALL_ID, zoneId, T3, T4, ADMIN_ID);
    }

    @Test
    void createRejectsWhenApplicationPeriodInvalid() {
        CreateRecruitmentNoticeRequest request =
                new CreateRecruitmentNoticeRequest(REQUEST_ID, "제목", "내용", null, null, T2, T1);

        assertThatThrownBy(() -> service.create(ADMIN_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPLICATION_PERIOD_INVALID);
        verify(recruitmentNoticeRequestRepository, never()).findById(any());
    }

    @Test
    void createRejectsWhenNoticeRequestNotFound() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_REQUEST_NOT_FOUND);
    }

    @Test
    void createRejectsWhenVenueDecisionNotAllowed() {
        RecruitmentNoticeRequest pending =
                RecruitmentNoticeRequest.create(HOST_CLIENT_ID, "제목", "설명", T1, T2, T3, T4, 1L);
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_CREATION_NOT_ALLOWED);
    }

    @Test
    void createRejectsWhenNoticeAlreadyExistsForRequest() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(recruitmentNoticeRepository.existsByRequestId(REQUEST_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_RECRUITMENT_NOTICE_REQUEST);
    }

    /** 홀 확정 단계(장소 예약)가 아직 안 끝난 요청은 공고를 만들 수 없어야 한다. */
    @Test
    void createRejectsWhenNoReservationsExist() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(recruitmentNoticeRepository.existsByRequestId(REQUEST_ID)).thenReturn(false);
        when(venueReservationRepository.findAllByNoticeRequestId(REQUEST_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_RESERVATION_NOT_FOUND);
    }

    /** 딸린 예약 중 하나라도 이미 해제됐다면 공고를 만들 수 없어야 한다. */
    @Test
    void createRejectsWhenAnyReservationAlreadyReleased() {
        VenueReservation confirmed = confirmedReservation(REQUEST_ID);
        VenueReservation released = confirmedReservation(REQUEST_ID);
        released.release();
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(recruitmentNoticeRepository.existsByRequestId(REQUEST_ID)).thenReturn(false);
        when(venueReservationRepository.findAllByNoticeRequestId(REQUEST_ID))
                .thenReturn(List.of(confirmed, released));

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_RESERVATION_ALREADY_RELEASED);
    }

    /** 예약이 여러 건이어도(구역 여러 개) 전부 이 공고에 연결돼야 한다 - 첫 건만 연결하는 회귀를 잡는다. */
    @Test
    void createSucceedsAsDraftAndLinksAllReservations() throws ReflectiveOperationException {
        VenueReservation reservation1 = confirmedReservation(REQUEST_ID, ZONE_ID);
        VenueReservation reservation2 = confirmedReservation(REQUEST_ID, OTHER_ZONE_ID);
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(recruitmentNoticeRepository.existsByRequestId(REQUEST_ID)).thenReturn(false);
        when(venueReservationRepository.findAllByNoticeRequestId(REQUEST_ID))
                .thenReturn(List.of(reservation1, reservation2));
        when(recruitmentNoticeRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            RecruitmentNotice notice = invocation.getArgument(0);
                            withId(notice, 99L);
                            return notice;
                        });

        RecruitmentNoticeResponse response = service.create(ADMIN_ID, createRequest());

        assertThat(response.status()).isEqualTo(RecruitmentNoticeStatus.DRAFT);
        assertThat(response.hostClientId()).isEqualTo(HOST_CLIENT_ID);
        assertThat(response.venueHallId()).isEqualTo(HALL_ID);
        assertThat(response.venueZoneIds()).containsExactlyInAnyOrder(ZONE_ID, OTHER_ZONE_ID);
        assertThat(reservation1.getRecruitmentNoticeId())
                .as("공고 생성 후 딸린 예약이 전부 이 공고에 연결돼야 한다")
                .isEqualTo(99L);
        assertThat(reservation2.getRecruitmentNoticeId())
                .as("공고 생성 후 딸린 예약이 전부 이 공고에 연결돼야 한다")
                .isEqualTo(99L);
        verify(recruitmentNoticeHistoryRepository)
                .save(
                        argThat(
                                (RecruitmentNoticeHistory history) ->
                                        history.getActionType()
                                                        == RecruitmentNoticeActionType.CREATE
                                                && Long.valueOf(99L)
                                                        .equals(history.getRecruitmentNoticeId())
                                                && ADMIN_ID.equals(history.getProcessedByAdminId())
                                                && "{\"status\": \"DRAFT\"}"
                                                        .equals(history.getAfterData())));
    }

    private static void withId(Object entity, Long id) throws ReflectiveOperationException {
        java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    private RecruitmentNotice draftNotice() {
        return RecruitmentNotice.create(REQUEST_ID, HOST_CLIENT_ID, "제목", "내용", T1, T2, ADMIN_ID);
    }

    /** 신청 시작일이 이미 지난 채로 게시(→OPEN)된 공고. OPEN 상태를 전제로 하는 테스트의 기본 픽스처. */
    private RecruitmentNotice openNotice() {
        RecruitmentNotice notice =
                RecruitmentNotice.create(
                        REQUEST_ID,
                        HOST_CLIENT_ID,
                        "제목",
                        "내용",
                        Instant.now().minusSeconds(3600),
                        T2,
                        ADMIN_ID);
        notice.publish();
        return notice;
    }

    @Test
    void updateRejectsWhenNotDraft() {
        RecruitmentNotice notice = openNotice();
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        UpdateRecruitmentNoticeRequest request =
                new UpdateRecruitmentNoticeRequest("새 제목", "새 내용", null, null, T1, T2);

        assertThatThrownBy(() -> service.update(1L, ADMIN_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_EDITABLE);
    }

    @Test
    void updateSucceedsWhenDraft() {
        RecruitmentNotice notice = draftNotice();
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        UpdateRecruitmentNoticeRequest request =
                new UpdateRecruitmentNoticeRequest("새 제목", "새 내용", null, null, T1, T2);

        RecruitmentNoticeResponse response = service.update(1L, ADMIN_ID, request);

        assertThat(response.title()).isEqualTo("새 제목");
        verify(recruitmentNoticeHistoryRepository)
                .save(
                        argThat(
                                (RecruitmentNoticeHistory history) ->
                                        history.getActionType()
                                                        == RecruitmentNoticeActionType.UPDATE
                                                && ADMIN_ID.equals(history.getProcessedByAdminId())
                                                && "{\"status\": \"DRAFT\"}"
                                                        .equals(history.getBeforeData())
                                                && "{\"status\": \"DRAFT\"}"
                                                        .equals(history.getAfterData())));
    }

    @Test
    void publishRejectsWhenNotDraft() {
        RecruitmentNotice notice = openNotice();
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        assertThatThrownBy(() -> service.publish(1L, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_PUBLISHABLE);
    }

    /**
     * 신청 시작일이 아직 안 된 공고를 게시하면, 바로 신청을 받는 OPEN 이 아니라 SCHEDULED 로만 전환돼야 한다 -
     * 시작일을 미래로 정한 의미가 없어지는 걸 막는다.
     */
    @Test
    void publishSucceedsFromDraft() {
        RecruitmentNotice notice = draftNotice();
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        RecruitmentNoticeResponse response = service.publish(1L, ADMIN_ID);

        assertThat(response.status()).isEqualTo(RecruitmentNoticeStatus.SCHEDULED);
        assertThat(response.publishedAt()).isNotNull();
        verify(recruitmentNoticeHistoryRepository)
                .save(
                        argThat(
                                (RecruitmentNoticeHistory history) ->
                                        history.getActionType()
                                                        == RecruitmentNoticeActionType.PUBLISH
                                                && ADMIN_ID.equals(history.getProcessedByAdminId())
                                                && "{\"status\": \"DRAFT\"}"
                                                        .equals(history.getBeforeData())
                                                && "{\"status\": \"SCHEDULED\"}"
                                                        .equals(history.getAfterData())));
    }

    /** 신청 시작일이 이미 지난 공고는 게시하는 즉시 OPEN 이 되어야 한다(예약 단계를 거칠 필요가 없다). */
    @Test
    void publishOpensImmediatelyWhenApplicationAlreadyStarted() {
        RecruitmentNotice notice =
                RecruitmentNotice.create(
                        REQUEST_ID,
                        HOST_CLIENT_ID,
                        "제목",
                        "내용",
                        Instant.now().minusSeconds(3600),
                        T2,
                        ADMIN_ID);
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        RecruitmentNoticeResponse response = service.publish(1L, ADMIN_ID);

        assertThat(response.status()).isEqualTo(RecruitmentNoticeStatus.OPEN);
    }

    @Test
    void closeRejectsWhenNotOpen() {
        RecruitmentNotice notice = draftNotice();
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        assertThatThrownBy(() -> service.close(1L, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_CLOSABLE);
    }

    @Test
    void closeSucceedsFromOpen() {
        RecruitmentNotice notice = openNotice();
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        RecruitmentNoticeResponse response = service.close(1L, ADMIN_ID);

        assertThat(response.status()).isEqualTo(RecruitmentNoticeStatus.CLOSED);
        verify(recruitmentNoticeHistoryRepository)
                .save(
                        argThat(
                                (RecruitmentNoticeHistory history) ->
                                        history.getActionType() == RecruitmentNoticeActionType.CLOSE
                                                && ADMIN_ID.equals(history.getProcessedByAdminId())
                                                && "{\"status\": \"OPEN\"}"
                                                        .equals(history.getBeforeData())
                                                && "{\"status\": \"CLOSED\"}"
                                                        .equals(history.getAfterData())));
    }

    @Test
    void cancelRejectsWhenClosed() {
        RecruitmentNotice notice = openNotice();
        notice.close();
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        assertThatThrownBy(() -> service.cancel(1L, ADMIN_ID, "테스트 취소"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_CANCELABLE);
        verify(recruitmentNoticeHistoryRepository, never()).save(any());
    }

    /**
     * 결제 완료(SUBMITTED) 신청서가 있으면 공고 상태와 무관하게 취소를 거부해야 한다 - 안 그러면 이미 돈을 낸
     * 기업이 있는 채로 공고가 사라져 배정·환불이 방치된다.
     */
    @Test
    void cancelRejectsWhenSubmittedApplicationsExist() {
        RecruitmentNotice notice = openNotice();
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        when(participationApplicationRepository.existsByRecruitmentNoticeIdAndStatus(
                        1L, ParticipationApplicationStatus.SUBMITTED))
                .thenReturn(true);

        assertThatThrownBy(() -> service.cancel(1L, ADMIN_ID, "테스트 취소"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_HAS_SUBMITTED_APPLICATIONS);
        verify(recruitmentNoticeHistoryRepository, never()).save(any());
        verify(venueReservationRepository, never()).findAllByRecruitmentNoticeId(any());
    }

    /** 예약이 여러 건이어도(구역 여러 개) 전부 해제돼야 한다 - 첫 건만 해제하는 회귀를 잡는다. */
    @Test
    void cancelSucceedsFromOpenAndLogsHistoryAndReleasesReservations() {
        RecruitmentNotice notice = openNotice();
        VenueReservation reservation1 = confirmedReservation(REQUEST_ID, ZONE_ID);
        VenueReservation reservation2 = confirmedReservation(REQUEST_ID, OTHER_ZONE_ID);
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        when(venueReservationRepository.findAllByRecruitmentNoticeId(1L))
                .thenReturn(List.of(reservation1, reservation2));

        RecruitmentNoticeResponse response = service.cancel(1L, ADMIN_ID, "테스트 취소");

        assertThat(response.status()).isEqualTo(RecruitmentNoticeStatus.CANCELED);
        verify(recruitmentNoticeHistoryRepository).save(any());
        assertThat(reservation1.getStatus())
                .as("공고 취소 시 딸린 예약이 전부 같이 해제돼야 한다")
                .isEqualTo(VenueReservationStatus.RELEASED);
        assertThat(reservation2.getStatus())
                .as("공고 취소 시 딸린 예약이 전부 같이 해제돼야 한다")
                .isEqualTo(VenueReservationStatus.RELEASED);
        verify(venueReservationHistoryRepository, times(2))
                .save(
                        argThat(
                                (VenueReservationHistory history) ->
                                        history.getActionType()
                                                        == VenueReservationActionType.RELEASED
                                                && "모집공고 취소: 테스트 취소".equals(history.getReason())
                                                && ADMIN_ID.equals(
                                                        history.getProcessedByAdminId())));
    }

    /** 취소 사유를 안 넣어도(reason == null) 이력에 문자열 "null" 이 그대로 붙으면 안 된다. */
    @Test
    void cancelWithoutReasonDoesNotLeakNullIntoReleaseHistoryReason() {
        RecruitmentNotice notice = openNotice();
        VenueReservation reservation = confirmedReservation(REQUEST_ID, ZONE_ID);
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        when(venueReservationRepository.findAllByRecruitmentNoticeId(1L))
                .thenReturn(List.of(reservation));

        service.cancel(1L, ADMIN_ID, null);

        verify(venueReservationHistoryRepository)
                .save(
                        argThat(
                                (VenueReservationHistory history) ->
                                        "모집공고 취소".equals(history.getReason())));
    }

    @Test
    void listPublicReturnsOnlyOpenNotices() {
        RecruitmentNotice published = openNotice();
        when(recruitmentNoticeRepository.findAllByStatus(RecruitmentNoticeStatus.OPEN))
                .thenReturn(List.of(published));

        assertThat(service.listPublic()).hasSize(1);
        assertThat(service.listPublic().get(0).status()).isEqualTo(RecruitmentNoticeStatus.OPEN);
    }

    /** 초안·마감 등 게시 중이 아닌 공고는 공개 상세 조회에서 볼 수 없어야 한다. */
    @Test
    void getPublicRejectsWhenNotOpen() {
        when(recruitmentNoticeRepository.findByIdAndStatus(1L, RecruitmentNoticeStatus.OPEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublic(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_FOUND);
    }

    @Test
    void getPublicReturnsOpenNotice() {
        RecruitmentNotice published = openNotice();
        when(recruitmentNoticeRepository.findByIdAndStatus(1L, RecruitmentNoticeStatus.OPEN))
                .thenReturn(Optional.of(published));

        RecruitmentNoticeResponse response = service.getPublic(1L);

        assertThat(response.status()).isEqualTo(RecruitmentNoticeStatus.OPEN);
    }

    /** 신청 시작일이 지난 SCHEDULED 공고는 일괄 처리에서 OPEN 으로 전환돼야 한다. */
    @Test
    void processScheduleActivatesScheduledNoticePastStartDate()
            throws ReflectiveOperationException {
        RecruitmentNotice notice = draftNotice();
        notice.publish();
        withId(notice, 5L);
        when(recruitmentNoticeRepository.findAllByStatusAndApplicationStartAtBefore(
                        eq(RecruitmentNoticeStatus.SCHEDULED), any()))
                .thenReturn(List.of(notice));
        when(recruitmentNoticeRepository.findAllByStatusAndApplicationEndAtBefore(
                        eq(RecruitmentNoticeStatus.OPEN), any()))
                .thenReturn(List.of());

        var result = service.processSchedule();

        assertThat(result.activatedNoticeIds()).containsExactly(5L);
        assertThat(result.expiredNoticeIds()).isEmpty();
        assertThat(notice.getStatus()).isEqualTo(RecruitmentNoticeStatus.OPEN);
        verify(recruitmentNoticeHistoryRepository)
                .save(
                        argThat(
                                (RecruitmentNoticeHistory history) ->
                                        history.getActionType()
                                                        == RecruitmentNoticeActionType.ACTIVATE
                                                && Long.valueOf(5L)
                                                        .equals(history.getRecruitmentNoticeId())
                                                && ADMIN_ID.equals(
                                                        history.getProcessedByAdminId())));
    }

    /** 신청 종료일이 지난 OPEN 공고는 일괄 처리에서 CLOSED 로 자동 마감돼야 한다. */
    @Test
    void processScheduleExpiresOpenNoticePastEndDate() throws ReflectiveOperationException {
        RecruitmentNotice notice = openNotice();
        withId(notice, 7L);
        when(recruitmentNoticeRepository.findAllByStatusAndApplicationStartAtBefore(
                        eq(RecruitmentNoticeStatus.SCHEDULED), any()))
                .thenReturn(List.of());
        when(recruitmentNoticeRepository.findAllByStatusAndApplicationEndAtBefore(
                        eq(RecruitmentNoticeStatus.OPEN), any()))
                .thenReturn(List.of(notice));

        var result = service.processSchedule();

        assertThat(result.expiredNoticeIds()).containsExactly(7L);
        assertThat(result.activatedNoticeIds()).isEmpty();
        assertThat(notice.getStatus()).isEqualTo(RecruitmentNoticeStatus.CLOSED);
        verify(recruitmentNoticeHistoryRepository)
                .save(
                        argThat(
                                (RecruitmentNoticeHistory history) ->
                                        history.getActionType() == RecruitmentNoticeActionType.CLOSE
                                                && Long.valueOf(7L)
                                                        .equals(history.getRecruitmentNoticeId())
                                                && ADMIN_ID.equals(
                                                        history.getProcessedByAdminId())));
    }

    @Test
    void processScheduleReturnsEmptyWhenNothingDue() {
        when(recruitmentNoticeRepository.findAllByStatusAndApplicationStartAtBefore(
                        eq(RecruitmentNoticeStatus.SCHEDULED), any()))
                .thenReturn(List.of());
        when(recruitmentNoticeRepository.findAllByStatusAndApplicationEndAtBefore(
                        eq(RecruitmentNoticeStatus.OPEN), any()))
                .thenReturn(List.of());

        var result = service.processSchedule();

        assertThat(result.activatedNoticeIds()).isEmpty();
        assertThat(result.expiredNoticeIds()).isEmpty();
    }
}
