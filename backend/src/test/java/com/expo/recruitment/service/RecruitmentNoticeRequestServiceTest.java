package com.expo.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.recruitment.converter.RecruitmentNoticeRequestConverter;
import com.expo.recruitment.dto.CreateRecruitmentNoticeRequestRequest;
import com.expo.recruitment.dto.DecideVenueRequest;
import com.expo.recruitment.dto.RecruitmentNoticeRequestResponse;
import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import com.expo.recruitment.entity.RecruitmentNoticeRequestActionType;
import com.expo.recruitment.entity.RecruitmentNoticeRequestHistory;
import com.expo.recruitment.entity.RecruitmentNoticeRequestStatus;
import com.expo.recruitment.entity.VenueDecision;
import com.expo.recruitment.repository.RecruitmentNoticeRequestHistoryRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRequestRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRequestZoneRepository;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VenueZoneRepository;
import com.expo.venue.repository.VirtualVenueRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link RecruitmentNoticeRequestService} 의 기간·장소 계층 검증과 장소 충돌 판정 규칙을 검증한다. */
class RecruitmentNoticeRequestServiceTest {

    private static final Long HOST_CLIENT_ID = 1L;
    private static final Long ADMIN_ID = 2L;
    private static final Long VENUE_ID = 100L;
    private static final Long HALL_ID = 200L;
    private static final Long ZONE_ID = 300L;
    private static final Long OTHER_ZONE_ID = 301L;
    private static final Instant T1 = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-09-30T00:00:00Z");
    private static final Instant T3 = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant T4 = Instant.parse("2026-10-05T00:00:00Z");

    private RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository;
    private RecruitmentNoticeRequestZoneRepository recruitmentNoticeRequestZoneRepository;
    private RecruitmentNoticeRequestHistoryRepository recruitmentNoticeRequestHistoryRepository;
    private VirtualVenueRepository virtualVenueRepository;
    private VenueHallRepository venueHallRepository;
    private VenueZoneRepository venueZoneRepository;
    private RecruitmentNoticeRequestService service;

    @BeforeEach
    void setUp() {
        recruitmentNoticeRequestRepository = mock(RecruitmentNoticeRequestRepository.class);
        recruitmentNoticeRequestZoneRepository = mock(RecruitmentNoticeRequestZoneRepository.class);
        recruitmentNoticeRequestHistoryRepository =
                mock(RecruitmentNoticeRequestHistoryRepository.class);
        virtualVenueRepository = mock(VirtualVenueRepository.class);
        venueHallRepository = mock(VenueHallRepository.class);
        venueZoneRepository = mock(VenueZoneRepository.class);
        service =
                new RecruitmentNoticeRequestService(
                        recruitmentNoticeRequestRepository,
                        recruitmentNoticeRequestZoneRepository,
                        recruitmentNoticeRequestHistoryRepository,
                        virtualVenueRepository,
                        venueHallRepository,
                        venueZoneRepository,
                        new RecruitmentNoticeRequestConverter());
    }

    private CreateRecruitmentNoticeRequestRequest requestWith(Long hallId, List<Long> zoneIds) {
        return new CreateRecruitmentNoticeRequestRequest(
                "제목", "설명", T1, T2, T3, T4, VENUE_ID, hallId, zoneIds, null, null);
    }

    @Test
    void createRejectsWhenApplicationPeriodInvalid() {
        CreateRecruitmentNoticeRequestRequest request =
                new CreateRecruitmentNoticeRequestRequest(
                        "제목",
                        "설명",
                        T2,
                        T1,
                        T3,
                        T4,
                        VENUE_ID,
                        HALL_ID,
                        List.of(ZONE_ID),
                        null,
                        null);

        assertThatThrownBy(() -> service.create(HOST_CLIENT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPLICATION_PERIOD_INVALID);
        verify(virtualVenueRepository, never()).existsById(any());
    }

    @Test
    void createRejectsWhenEventPeriodInvalid() {
        CreateRecruitmentNoticeRequestRequest request =
                new CreateRecruitmentNoticeRequestRequest(
                        "제목",
                        "설명",
                        T1,
                        T2,
                        T4,
                        T3,
                        VENUE_ID,
                        HALL_ID,
                        List.of(ZONE_ID),
                        null,
                        null);

        assertThatThrownBy(() -> service.create(HOST_CLIENT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVENT_PERIOD_INVALID);
    }

    @Test
    void createRejectsWhenVenueNotFound() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.create(
                                        HOST_CLIENT_ID, requestWith(HALL_ID, List.of(ZONE_ID))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VIRTUAL_VENUE_NOT_FOUND);
    }

    @Test
    void createRejectsWhenHallDoesNotBelongToVenue() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        when(venueHallRepository.existsByIdAndVenueId(HALL_ID, VENUE_ID)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.create(
                                        HOST_CLIENT_ID, requestWith(HALL_ID, List.of(ZONE_ID))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_HALL_ZONE_MISMATCH);
    }

    @Test
    void createRejectsWhenZoneDoesNotBelongToHall() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        when(venueHallRepository.existsByIdAndVenueId(HALL_ID, VENUE_ID)).thenReturn(true);
        when(venueZoneRepository.existsByIdAndHallId(ZONE_ID, HALL_ID)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.create(
                                        HOST_CLIENT_ID, requestWith(HALL_ID, List.of(ZONE_ID))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_HALL_ZONE_MISMATCH);
    }

    @Test
    void createSucceedsAsDraftWithClearAndPendingAndSavesEachZone() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        when(venueHallRepository.existsByIdAndVenueId(HALL_ID, VENUE_ID)).thenReturn(true);
        when(venueZoneRepository.existsByIdAndHallId(ZONE_ID, HALL_ID)).thenReturn(true);
        when(venueZoneRepository.existsByIdAndHallId(OTHER_ZONE_ID, HALL_ID)).thenReturn(true);
        when(recruitmentNoticeRequestRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecruitmentNoticeRequestResponse response =
                service.create(
                        HOST_CLIENT_ID, requestWith(HALL_ID, List.of(ZONE_ID, OTHER_ZONE_ID)));

        assertThat(response.status()).isEqualTo(RecruitmentNoticeRequestStatus.DRAFT);
        assertThat(response.venueDecision()).isEqualTo(VenueDecision.PENDING);
        assertThat(response.hostClientId()).isEqualTo(HOST_CLIENT_ID);
        assertThat(response.venueZoneIds()).containsExactlyInAnyOrder(ZONE_ID, OTHER_ZONE_ID);
        verify(recruitmentNoticeRequestZoneRepository, times(2)).save(any());
    }

    @Test
    void decideVenueRejectsInvalidDecision() {
        DecideVenueRequest request = new DecideVenueRequest(VenueDecision.PENDING, "사유");

        assertThatThrownBy(() -> service.decideVenue(1L, ADMIN_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_DECISION_INVALID);
    }

    @Test
    void decideVenueRejectsWhenRequestNotFound() {
        when(recruitmentNoticeRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        DecideVenueRequest request = new DecideVenueRequest(VenueDecision.ALLOWED, "사유");

        assertThatThrownBy(() -> service.decideVenue(1L, ADMIN_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_REQUEST_NOT_FOUND);
    }

    @Test
    void decideVenueRejectsWhenAlreadyDecided() {
        RecruitmentNoticeRequest entity = draftRequest();
        entity.decideVenue(VenueDecision.ALLOWED, ADMIN_ID, "이전 승인");
        when(recruitmentNoticeRequestRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(entity));
        DecideVenueRequest request = new DecideVenueRequest(VenueDecision.CANCELED, "사유");

        assertThatThrownBy(() -> service.decideVenue(1L, ADMIN_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_DECISION_ALREADY_MADE);
        verify(recruitmentNoticeRequestHistoryRepository, never()).save(any());
    }

    @Test
    void decideVenueAllowedApprovesRequest() {
        RecruitmentNoticeRequest entity = draftRequest();
        when(recruitmentNoticeRequestRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(entity));
        DecideVenueRequest request = new DecideVenueRequest(VenueDecision.ALLOWED, "충돌 없음");

        RecruitmentNoticeRequestResponse response = service.decideVenue(1L, ADMIN_ID, request);

        assertThat(response.venueDecision()).isEqualTo(VenueDecision.ALLOWED);
        assertThat(response.status()).isEqualTo(RecruitmentNoticeRequestStatus.APPROVED);
        verify(recruitmentNoticeRequestHistoryRepository)
                .save(
                        argThat(
                                (RecruitmentNoticeRequestHistory history) ->
                                        history.getActionType()
                                                        == RecruitmentNoticeRequestActionType
                                                                .VENUE_ALLOW
                                                && "PENDING".equals(history.getFromStatus())
                                                && "ALLOWED".equals(history.getToStatus())
                                                && "충돌 없음".equals(history.getReason())
                                                && ADMIN_ID.equals(
                                                        history.getProcessedByAdminId())));
    }

    @Test
    void decideVenueCanceledRejectsRequest() {
        RecruitmentNoticeRequest entity = draftRequest();
        when(recruitmentNoticeRequestRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(entity));
        DecideVenueRequest request = new DecideVenueRequest(VenueDecision.CANCELED, "충돌 발생");

        RecruitmentNoticeRequestResponse response = service.decideVenue(1L, ADMIN_ID, request);

        assertThat(response.venueDecision()).isEqualTo(VenueDecision.CANCELED);
        assertThat(response.status()).isEqualTo(RecruitmentNoticeRequestStatus.REJECTED);
        verify(recruitmentNoticeRequestHistoryRepository)
                .save(
                        argThat(
                                (RecruitmentNoticeRequestHistory history) ->
                                        history.getActionType()
                                                        == RecruitmentNoticeRequestActionType
                                                                .VENUE_CANCEL
                                                && "PENDING".equals(history.getFromStatus())
                                                && "CANCELED".equals(history.getToStatus())
                                                && "충돌 발생".equals(history.getReason())));
    }

    private RecruitmentNoticeRequest draftRequest() {
        return RecruitmentNoticeRequest.create(
                HOST_CLIENT_ID, "제목", "설명", T1, T2, T3, T4, VENUE_ID);
    }
}
