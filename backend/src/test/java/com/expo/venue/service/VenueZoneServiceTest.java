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
import com.expo.venue.converter.VenueZoneConverter;
import com.expo.venue.dto.CreateVenueZoneRequest;
import com.expo.venue.dto.UpdateVenueLayoutRequest;
import com.expo.venue.dto.VenueZoneResponse;
import com.expo.venue.entity.VenueZone;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VenueZoneRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** {@link VenueZoneService} 의 상위 홀 존재 여부·구역 코드 중복 검증을 확인한다. */
class VenueZoneServiceTest {

    private static final Long HALL_ID = 1L;

    private VenueZoneRepository venueZoneRepository;
    private VenueHallRepository venueHallRepository;
    private VenueZoneService service;

    @BeforeEach
    void setUp() {
        venueZoneRepository = mock(VenueZoneRepository.class);
        venueHallRepository = mock(VenueHallRepository.class);
        service =
                new VenueZoneService(
                        venueZoneRepository, venueHallRepository, new VenueZoneConverter());
    }

    private CreateVenueZoneRequest request() {
        return new CreateVenueZoneRequest(
                "ZONE-1", "1구역", 50, BigDecimal.TEN, BigDecimal.TEN, null);
    }

    @Test
    void createRejectsWhenHallNotFound() {
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(HALL_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_HALL_NOT_FOUND);
        verify(venueZoneRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsDuplicateZoneCode() {
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        when(venueZoneRepository.existsByHallIdAndZoneCode(HALL_ID, "ZONE-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(HALL_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_VENUE_ZONE_CODE);
    }

    @Test
    void createSucceeds() {
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        when(venueZoneRepository.existsByHallIdAndZoneCode(HALL_ID, "ZONE-1")).thenReturn(false);
        when(venueZoneRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VenueZoneResponse response = service.create(HALL_ID, request());

        assertThat(response.hallId()).isEqualTo(HALL_ID);
        assertThat(response.zoneCode()).isEqualTo("ZONE-1");
        verify(venueZoneRepository).saveAndFlush(any());
    }

    /** 같은 홀에 구역이 이미 5개(킨텍스 1~5홀) 있으면 더 등록할 수 없다. */
    @Test
    void createRejectsWhenZoneLimitReached() {
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        when(venueZoneRepository.existsByHallIdAndZoneCode(HALL_ID, "ZONE-1")).thenReturn(false);
        when(venueZoneRepository.countByHallId(HALL_ID)).thenReturn(5L);

        assertThatThrownBy(() -> service.create(HALL_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_ZONE_LIMIT_EXCEEDED);
        verify(venueZoneRepository, never()).saveAndFlush(any());
    }

    /** 사전 개수 검사를 통과해도 동시 삽입으로 트리거가 막으면 같은 오류로 변환돼야 한다. */
    @Test
    void createTranslatesLimitTriggerViolationToLimitExceeded() {
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        when(venueZoneRepository.existsByHallIdAndZoneCode(HALL_ID, "ZONE-1")).thenReturn(false);
        when(venueZoneRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("ERROR: venue_zone_limit_exceeded"));

        assertThatThrownBy(() -> service.create(HALL_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_ZONE_LIMIT_EXCEEDED);
    }

    private static void withId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void listRejectsWhenHallNotFound() {
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.list(HALL_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_HALL_NOT_FOUND);
    }

    @Test
    void listReturnsZonesInHall() {
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        VenueZone zone =
                VenueZone.create(
                        HALL_ID, "ZONE-1", "1구역", 50, BigDecimal.TEN, BigDecimal.TEN, null);
        withId(zone, 10L);
        when(venueZoneRepository.findAllByHallIdOrderByZoneCodeAsc(HALL_ID))
                .thenReturn(List.of(zone));

        List<VenueZoneResponse> responses = service.list(HALL_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(10L);
    }

    @Test
    void getRejectsWhenHallNotFound() {
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.get(HALL_ID, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_HALL_NOT_FOUND);
        verify(venueZoneRepository, never()).findByIdAndHallId(any(), any());
    }

    @Test
    void getRejectsWhenNotFoundInHall() {
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        when(venueZoneRepository.findByIdAndHallId(10L, HALL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(HALL_ID, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_ZONE_NOT_FOUND);
    }

    @Test
    void getSucceeds() {
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        VenueZone zone =
                VenueZone.create(
                        HALL_ID, "ZONE-1", "1구역", 50, BigDecimal.TEN, BigDecimal.TEN, null);
        withId(zone, 10L);
        when(venueZoneRepository.findByIdAndHallId(10L, HALL_ID)).thenReturn(Optional.of(zone));

        VenueZoneResponse response = service.get(HALL_ID, 10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.zoneCode()).isEqualTo("ZONE-1");
    }

    @Test
    void updateLayoutRejectsWhenHallNotFound() {
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(false);

        assertThatThrownBy(
                        () -> service.updateLayout(HALL_ID, 10L, new UpdateVenueLayoutRequest(99L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_HALL_NOT_FOUND);
    }

    @Test
    void updateLayoutRejectsWhenZoneNotFound() {
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        when(venueZoneRepository.findByIdAndHallId(10L, HALL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> service.updateLayout(HALL_ID, 10L, new UpdateVenueLayoutRequest(99L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_ZONE_NOT_FOUND);
    }

    @Test
    void updateLayoutReplacesLayoutFileId() {
        when(venueHallRepository.existsById(HALL_ID)).thenReturn(true);
        VenueZone zone =
                VenueZone.create(
                        HALL_ID, "ZONE-1", "1구역", 50, BigDecimal.TEN, BigDecimal.TEN, null);
        withId(zone, 10L);
        when(venueZoneRepository.findByIdAndHallId(10L, HALL_ID)).thenReturn(Optional.of(zone));

        VenueZoneResponse response =
                service.updateLayout(HALL_ID, 10L, new UpdateVenueLayoutRequest(99L));

        assertThat(response.layoutFileId()).isEqualTo(99L);
    }
}
