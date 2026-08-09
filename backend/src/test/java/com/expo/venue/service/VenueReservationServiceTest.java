package com.expo.venue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import com.expo.recruitment.entity.VenueDecision;
import com.expo.recruitment.repository.RecruitmentNoticeRequestRepository;
import com.expo.venue.converter.VenueReservationConverter;
import com.expo.venue.dto.CreateVenueReservationRequest;
import com.expo.venue.dto.VenueAvailabilityResponse;
import com.expo.venue.dto.VenueReservationResponse;
import com.expo.venue.entity.VenueReservation;
import com.expo.venue.entity.VenueReservationStatus;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VenueReservationRepository;
import com.expo.venue.repository.VenueZoneRepository;
import com.expo.venue.repository.VirtualVenueRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** {@link VenueReservationService} 의 승인 상태·계층 검증·기간 중복 처리 규칙을 검증한다. */
class VenueReservationServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long REQUEST_ID = 10L;
    private static final Long VENUE_ID = 100L;
    private static final Long HALL_ID = 200L;
    private static final Long ZONE_ID = 300L;
    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 10, 5, 0, 0);

    private VenueReservationRepository venueReservationRepository;
    private RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository;
    private VirtualVenueRepository virtualVenueRepository;
    private VenueHallRepository venueHallRepository;
    private VenueZoneRepository venueZoneRepository;
    private VenueReservationService service;

    @BeforeEach
    void setUp() {
        venueReservationRepository = mock(VenueReservationRepository.class);
        recruitmentNoticeRequestRepository = mock(RecruitmentNoticeRequestRepository.class);
        virtualVenueRepository = mock(VirtualVenueRepository.class);
        venueHallRepository = mock(VenueHallRepository.class);
        venueZoneRepository = mock(VenueZoneRepository.class);
        service =
                new VenueReservationService(
                        venueReservationRepository,
                        recruitmentNoticeRequestRepository,
                        virtualVenueRepository,
                        venueHallRepository,
                        venueZoneRepository,
                        new VenueReservationConverter());
    }

    private CreateVenueReservationRequest requestWith(Long hallId, Long zoneId) {
        return new CreateVenueReservationRequest(REQUEST_ID, VENUE_ID, hallId, zoneId, START, END);
    }

    private RecruitmentNoticeRequest allowedNoticeRequest() {
        return noticeRequestWithDecision(VenueDecision.ALLOWED);
    }

    /** 이 브랜치의 엔티티에는 아직 팩토리 메서드가 없어 리플렉션으로 venueDecision 만 세팅한다. */
    private RecruitmentNoticeRequest noticeRequestWithDecision(VenueDecision decision) {
        try {
            java.lang.reflect.Constructor<RecruitmentNoticeRequest> constructor =
                    RecruitmentNoticeRequest.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            RecruitmentNoticeRequest entity = constructor.newInstance();
            java.lang.reflect.Field field =
                    RecruitmentNoticeRequest.class.getDeclaredField("venueDecision");
            field.setAccessible(true);
            field.set(entity, decision);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void createRejectsWhenEndNotAfterStart() {
        CreateVenueReservationRequest request =
                new CreateVenueReservationRequest(REQUEST_ID, VENUE_ID, null, null, END, START);

        assertThatThrownBy(() -> service.create(ADMIN_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_RESERVATION_PERIOD_INVALID);
    }

    @Test
    void createRejectsWhenNoticeRequestNotFound() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_REQUEST_NOT_FOUND);
    }

    @Test
    void createRejectsWhenVenueDecisionNotAllowed() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(noticeRequestWithDecision(VenueDecision.PENDING)));

        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_REQUEST_NOT_ALLOWED);
        verify(virtualVenueRepository, never()).existsById(any());
    }

    @Test
    void createRejectsWhenVirtualVenueNotFound() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VIRTUAL_VENUE_NOT_FOUND);
    }

    @Test
    void createRejectsWhenHallDoesNotBelongToVenue() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        when(venueHallRepository.existsByIdAndVenueId(HALL_ID, VENUE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(HALL_ID, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_HALL_ZONE_MISMATCH);
    }

    @Test
    void createSucceedsAndReturnsConfirmedReservation() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueReservationRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VenueReservationResponse response = service.create(ADMIN_ID, requestWith(null, null));

        assertThat(response.status()).isEqualTo(VenueReservationStatus.CONFIRMED);
        assertThat(response.noticeRequestId()).isEqualTo(REQUEST_ID);
        assertThat(response.confirmedByAdminId()).isEqualTo(ADMIN_ID);
    }

    /** DB EXCLUDE 제약({@code ex_venue_reservations_period}) 위반은 기간 중복 오류로 변환돼야 한다. */
    @Test
    void createTranslatesConstraintViolationToPeriodConflict() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueReservationRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("exclusion violation"));

        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_RESERVATION_PERIOD_CONFLICT);
    }

    @Test
    void releaseRejectsWhenReservationNotFound() {
        when(venueReservationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.release(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_RESERVATION_NOT_FOUND);
    }

    @Test
    void releaseRejectsWhenAlreadyReleased() {
        VenueReservation reservation = confirmedReservation();
        reservation.release();
        when(venueReservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.release(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_RESERVATION_ALREADY_RELEASED);
    }

    @Test
    void releaseSucceedsOnConfirmedReservation() {
        VenueReservation reservation = confirmedReservation();
        when(venueReservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        VenueReservationResponse response = service.release(1L);

        assertThat(response.status()).isEqualTo(VenueReservationStatus.RELEASED);
    }

    @Test
    void checkAvailabilityReturnsFalseWhenOverlapping() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueReservationRepository.existsOverlapping(VENUE_ID, null, null, START, END))
                .thenReturn(true);

        VenueAvailabilityResponse response =
                service.checkAvailability(VENUE_ID, null, null, START, END);

        assertThat(response.available()).isFalse();
    }

    @Test
    void checkAvailabilityReturnsTrueWhenNotOverlapping() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueReservationRepository.existsOverlapping(VENUE_ID, null, null, START, END))
                .thenReturn(false);

        VenueAvailabilityResponse response =
                service.checkAvailability(VENUE_ID, null, null, START, END);

        assertThat(response.available()).isTrue();
    }

    private VenueReservation confirmedReservation() {
        return VenueReservation.confirmForRecruitmentNotice(
                REQUEST_ID, VENUE_ID, HALL_ID, ZONE_ID, START, END, ADMIN_ID);
    }
}
