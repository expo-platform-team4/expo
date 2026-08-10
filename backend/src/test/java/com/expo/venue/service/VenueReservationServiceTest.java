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
import com.expo.venue.entity.VenueZone;
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
    void createRejectsWhenZoneDoesNotBelongToGivenHall() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        Long otherHallId = 999L;
        when(venueZoneRepository.findById(ZONE_ID))
                .thenReturn(Optional.of(zoneOf(ZONE_ID, HALL_ID)));

        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(otherHallId, ZONE_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_HALL_ZONE_MISMATCH);
    }

    /** 홀 없이 구역만 지정해도 구역이 속한 홀을 조회해서 정상 처리돼야 한다 (버그 회귀 테스트). */
    @Test
    void createSucceedsWithZoneOnlyResolvesHallAutomatically() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueZoneRepository.findById(ZONE_ID))
                .thenReturn(Optional.of(zoneOf(ZONE_ID, HALL_ID)));
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        when(venueHallRepository.existsByIdAndVenueId(HALL_ID, VENUE_ID)).thenReturn(true);
        when(venueReservationRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VenueReservationResponse response = service.create(ADMIN_ID, requestWith(null, ZONE_ID));

        assertThat(response.status()).isEqualTo(VenueReservationStatus.CONFIRMED);
        assertThat(response.venueHallId())
                .as("구역만 지정해도 실제 저장되는 예약에는 구역이 속한 홀 ID가 채워져야 한다")
                .isEqualTo(HALL_ID);
    }

    @Test
    void createSucceedsWithHallAndMatchingZone() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueZoneRepository.findById(ZONE_ID))
                .thenReturn(Optional.of(zoneOf(ZONE_ID, HALL_ID)));
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        when(venueHallRepository.existsByIdAndVenueId(HALL_ID, VENUE_ID)).thenReturn(true);
        when(venueReservationRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VenueReservationResponse response = service.create(ADMIN_ID, requestWith(HALL_ID, ZONE_ID));

        assertThat(response.status()).isEqualTo(VenueReservationStatus.CONFIRMED);
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
                .thenThrow(
                        new DataIntegrityViolationException(
                                "duplicate key value violates exclusion constraint"
                                        + " \"ex_venue_reservations_period\""));

        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_RESERVATION_PERIOD_CONFLICT);
    }

    /** 기간 중복이 아닌 다른 무결성 위반은 그대로 다시 던져야 한다 (원인이 가려지면 안 된다). */
    @Test
    void createRethrowsUnrelatedConstraintViolation() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueReservationRepository.saveAndFlush(any()))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "insert or update violates foreign key constraint"));

        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(null, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
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
    void checkAvailabilityRejectsWhenEndNotAfterStart() {
        assertThatThrownBy(() -> service.checkAvailability(VENUE_ID, null, null, END, START))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_RESERVATION_PERIOD_INVALID);
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

    /** id 는 {@code @GeneratedValue} 라 팩토리로 못 채워서, 조회된 것처럼 리플렉션으로 세팅한다. */
    private VenueZone zoneOf(Long zoneId, Long hallId) {
        VenueZone zone = VenueZone.create(hallId, "ZONE-1", "1구역", 10, null, null, null);
        try {
            java.lang.reflect.Field field = VenueZone.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(zone, zoneId);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return zone;
    }
}
