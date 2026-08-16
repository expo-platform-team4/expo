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
import com.expo.recruitment.repository.RecruitmentNoticeRequestZoneRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** {@link VenueReservationService} 의 승인 상태 검증·구역별 다건 확정·기간 중복 처리 규칙을 검증한다. */
class VenueReservationServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long REQUEST_ID = 10L;
    private static final Long VENUE_ID = 100L;
    private static final Long HALL_ID = 200L;
    private static final Long ZONE_ID = 300L;
    private static final Long OTHER_ZONE_ID = 301L;
    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-10-05T00:00:00Z");

    private VenueReservationRepository venueReservationRepository;
    private RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository;
    private RecruitmentNoticeRequestZoneRepository recruitmentNoticeRequestZoneRepository;
    private VirtualVenueRepository virtualVenueRepository;
    private VenueHallRepository venueHallRepository;
    private VenueZoneRepository venueZoneRepository;
    private VenueReservationService service;

    @BeforeEach
    void setUp() {
        venueReservationRepository = mock(VenueReservationRepository.class);
        recruitmentNoticeRequestRepository = mock(RecruitmentNoticeRequestRepository.class);
        recruitmentNoticeRequestZoneRepository = mock(RecruitmentNoticeRequestZoneRepository.class);
        virtualVenueRepository = mock(VirtualVenueRepository.class);
        venueHallRepository = mock(VenueHallRepository.class);
        venueZoneRepository = mock(VenueZoneRepository.class);
        service =
                new VenueReservationService(
                        venueReservationRepository,
                        recruitmentNoticeRequestRepository,
                        recruitmentNoticeRequestZoneRepository,
                        virtualVenueRepository,
                        venueHallRepository,
                        venueZoneRepository,
                        new VenueReservationConverter());
    }

    private CreateVenueReservationRequest requestWith(Instant start, Instant end) {
        return new CreateVenueReservationRequest(REQUEST_ID, start, end);
    }

    private RecruitmentNoticeRequest allowedNoticeRequest() {
        return noticeRequestWithDecision(VenueDecision.ALLOWED);
    }

    /** 리플렉션으로 venueDecision·virtualVenueId·venueHallId 를 세팅한다 (팩토리로는 못 채우는 조회 시나리오). */
    private RecruitmentNoticeRequest noticeRequestWithDecision(VenueDecision decision) {
        try {
            java.lang.reflect.Constructor<RecruitmentNoticeRequest> constructor =
                    RecruitmentNoticeRequest.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            RecruitmentNoticeRequest entity = constructor.newInstance();
            setField(entity, "venueDecision", decision);
            setField(entity, "virtualVenueId", VENUE_ID);
            setField(entity, "venueHallId", HALL_ID);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value)
            throws ReflectiveOperationException {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void createRejectsWhenEndNotAfterStart() {
        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(END, START)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_RESERVATION_PERIOD_INVALID);
    }

    @Test
    void createRejectsWhenNoticeRequestNotFound() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(START, END)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_REQUEST_NOT_FOUND);
    }

    @Test
    void createRejectsWhenVenueDecisionNotAllowed() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(noticeRequestWithDecision(VenueDecision.PENDING)));

        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(START, END)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_REQUEST_NOT_ALLOWED);
        verify(recruitmentNoticeRequestZoneRepository, never()).findVenueZoneIdsByRequestId(any());
    }

    /** 승인은 됐지만 고른 구역이 하나도 없으면 예약을 만들 수 없어야 한다. */
    @Test
    void createRejectsWhenNoZonesSelected() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(recruitmentNoticeRequestZoneRepository.findVenueZoneIdsByRequestId(REQUEST_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(START, END)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_REQUEST_ZONES_EMPTY);
    }

    @Test
    void createSucceedsAndReturnsOneConfirmedReservationPerZone() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(recruitmentNoticeRequestZoneRepository.findVenueZoneIdsByRequestId(REQUEST_ID))
                .thenReturn(List.of(ZONE_ID, OTHER_ZONE_ID));
        when(venueReservationRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<VenueReservationResponse> responses =
                service.create(ADMIN_ID, requestWith(START, END));

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .allSatisfy(
                        response -> {
                            assertThat(response.status())
                                    .isEqualTo(VenueReservationStatus.CONFIRMED);
                            assertThat(response.noticeRequestId()).isEqualTo(REQUEST_ID);
                            assertThat(response.venueHallId()).isEqualTo(HALL_ID);
                            assertThat(response.confirmedByAdminId()).isEqualTo(ADMIN_ID);
                        });
        assertThat(responses.stream().map(VenueReservationResponse::venueZoneId))
                .containsExactlyInAnyOrder(ZONE_ID, OTHER_ZONE_ID);
    }

    /** DB EXCLUDE 제약({@code ex_venue_reservations_period}) 위반은 기간 중복 오류로 변환돼야 한다. */
    @Test
    void createTranslatesConstraintViolationToPeriodConflict() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(recruitmentNoticeRequestZoneRepository.findVenueZoneIdsByRequestId(REQUEST_ID))
                .thenReturn(List.of(ZONE_ID));
        when(venueReservationRepository.saveAndFlush(any()))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "duplicate key value violates exclusion constraint"
                                        + " \"ex_venue_reservations_period\""));

        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(START, END)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_RESERVATION_PERIOD_CONFLICT);
    }

    /**
     * 홀 전체 예약과 그 아래 구역 단위 예약이 겹칠 때 DB 트리거({@code
     * trg_venue_reservation_hierarchy_conflict})가 던지는 위반도 기간 중복 오류로 변환돼야 한다.
     */
    @Test
    void createTranslatesHierarchyConflictToPeriodConflict() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(recruitmentNoticeRequestZoneRepository.findVenueZoneIdsByRequestId(REQUEST_ID))
                .thenReturn(List.of(ZONE_ID));
        when(venueReservationRepository.saveAndFlush(any()))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "ERROR: venue_reservation_hierarchy_conflict"));

        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(START, END)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_RESERVATION_PERIOD_CONFLICT);
    }

    /** 기간 중복이 아닌 다른 무결성 위반은 그대로 다시 던져야 한다 (원인이 가려지면 안 된다). */
    @Test
    void createRethrowsUnrelatedConstraintViolation() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(recruitmentNoticeRequestZoneRepository.findVenueZoneIdsByRequestId(REQUEST_ID))
                .thenReturn(List.of(ZONE_ID));
        when(venueReservationRepository.saveAndFlush(any()))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "insert or update violates foreign key constraint"));

        assertThatThrownBy(() -> service.create(ADMIN_ID, requestWith(START, END)))
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

    /**
     * 구역만 지정하고 홀을 안 넘긴 조회는, 구역이 속한 홀로 겹침 여부를 확인해야 한다. 원본 요청의 null 홀 ID를 그대로 쓰면
     * 홀이 지정된 기존 예약과의 충돌을 못 잡는다 (버그 회귀 테스트).
     */
    @Test
    void checkAvailabilityUsesResolvedHallIdForZoneOnlyQuery() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueZoneRepository.findById(ZONE_ID))
                .thenReturn(Optional.of(zoneOf(ZONE_ID, HALL_ID)));
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        when(venueHallRepository.existsByIdAndVenueId(HALL_ID, VENUE_ID)).thenReturn(true);
        when(venueReservationRepository.existsOverlapping(VENUE_ID, HALL_ID, ZONE_ID, START, END))
                .thenReturn(true);

        VenueAvailabilityResponse response =
                service.checkAvailability(VENUE_ID, null, ZONE_ID, START, END);

        assertThat(response.available()).isFalse();
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
