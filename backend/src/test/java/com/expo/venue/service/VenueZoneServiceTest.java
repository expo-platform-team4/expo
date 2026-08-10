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
import com.expo.venue.dto.VenueZoneResponse;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VenueZoneRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
