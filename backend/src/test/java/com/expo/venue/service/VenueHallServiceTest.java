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
import com.expo.venue.converter.VenueHallConverter;
import com.expo.venue.dto.CreateVenueHallRequest;
import com.expo.venue.dto.UpdateVenueLayoutRequest;
import com.expo.venue.dto.VenueHallResponse;
import com.expo.venue.entity.VenueHall;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VirtualVenueRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** {@link VenueHallService} 의 상위 장소 존재 여부·홀 코드 중복 검증을 확인한다. */
class VenueHallServiceTest {

    private static final Long VENUE_ID = 1L;

    private VenueHallRepository venueHallRepository;
    private VirtualVenueRepository virtualVenueRepository;
    private VenueHallService service;

    @BeforeEach
    void setUp() {
        venueHallRepository = mock(VenueHallRepository.class);
        virtualVenueRepository = mock(VirtualVenueRepository.class);
        service =
                new VenueHallService(
                        venueHallRepository, virtualVenueRepository, new VenueHallConverter());
    }

    private CreateVenueHallRequest request() {
        return new CreateVenueHallRequest("HALL-A", "A홀", BigDecimal.TEN, BigDecimal.TEN, null);
    }

    @Test
    void createRejectsWhenVenueNotFound() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(VENUE_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VIRTUAL_VENUE_NOT_FOUND);
        verify(venueHallRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsDuplicateHallCode() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueHallRepository.existsByVenueIdAndHallCode(VENUE_ID, "HALL-A")).thenReturn(true);

        assertThatThrownBy(() -> service.create(VENUE_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_VENUE_HALL_CODE);
    }

    @Test
    void createSucceeds() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueHallRepository.existsByVenueIdAndHallCode(VENUE_ID, "HALL-A")).thenReturn(false);
        when(venueHallRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VenueHallResponse response = service.create(VENUE_ID, request());

        assertThat(response.venueId()).isEqualTo(VENUE_ID);
        assertThat(response.hallCode()).isEqualTo("HALL-A");
        verify(venueHallRepository).saveAndFlush(any());
    }

    /** 같은 장소에 홀이 이미 2개(킨텍스 제1·제2전시장) 있으면 더 등록할 수 없다. */
    @Test
    void createRejectsWhenHallLimitReached() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueHallRepository.existsByVenueIdAndHallCode(VENUE_ID, "HALL-A")).thenReturn(false);
        when(venueHallRepository.countByVenueId(VENUE_ID)).thenReturn(2L);

        assertThatThrownBy(() -> service.create(VENUE_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_HALL_LIMIT_EXCEEDED);
        verify(venueHallRepository, never()).saveAndFlush(any());
    }

    /** 사전 개수 검사를 통과해도 동시 삽입으로 트리거가 막으면 같은 오류로 변환돼야 한다. */
    @Test
    void createTranslatesLimitTriggerViolationToLimitExceeded() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueHallRepository.existsByVenueIdAndHallCode(VENUE_ID, "HALL-A")).thenReturn(false);
        when(venueHallRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("ERROR: venue_hall_limit_exceeded"));

        assertThatThrownBy(() -> service.create(VENUE_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_HALL_LIMIT_EXCEEDED);
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
    void listRejectsWhenVenueNotFound() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.list(VENUE_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VIRTUAL_VENUE_NOT_FOUND);
    }

    @Test
    void listReturnsHallsInVenue() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        VenueHall hall =
                VenueHall.create(VENUE_ID, "HALL-A", "A홀", BigDecimal.TEN, BigDecimal.TEN, null);
        withId(hall, 10L);
        when(venueHallRepository.findAllByVenueIdOrderByHallCodeAsc(VENUE_ID))
                .thenReturn(List.of(hall));

        List<VenueHallResponse> responses = service.list(VENUE_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(10L);
    }

    @Test
    void getRejectsWhenVenueNotFound() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.get(VENUE_ID, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VIRTUAL_VENUE_NOT_FOUND);
        verify(venueHallRepository, never()).findByIdAndVenueId(any(), any());
    }

    @Test
    void getRejectsWhenNotFoundInVenue() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueHallRepository.findByIdAndVenueId(10L, VENUE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(VENUE_ID, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_HALL_NOT_FOUND);
    }

    @Test
    void getSucceeds() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        VenueHall hall =
                VenueHall.create(VENUE_ID, "HALL-A", "A홀", BigDecimal.TEN, BigDecimal.TEN, null);
        withId(hall, 10L);
        when(venueHallRepository.findByIdAndVenueId(10L, VENUE_ID)).thenReturn(Optional.of(hall));

        VenueHallResponse response = service.get(VENUE_ID, 10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.hallCode()).isEqualTo("HALL-A");
    }

    @Test
    void updateLayoutRejectsWhenVenueNotFound() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.updateLayout(
                                        VENUE_ID, 10L, new UpdateVenueLayoutRequest(99L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VIRTUAL_VENUE_NOT_FOUND);
    }

    @Test
    void updateLayoutRejectsWhenHallNotFound() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        when(venueHallRepository.findByIdAndVenueId(10L, VENUE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.updateLayout(
                                        VENUE_ID, 10L, new UpdateVenueLayoutRequest(99L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_HALL_NOT_FOUND);
    }

    @Test
    void updateLayoutReplacesLayoutFileId() {
        when(virtualVenueRepository.existsById(VENUE_ID)).thenReturn(true);
        VenueHall hall =
                VenueHall.create(VENUE_ID, "HALL-A", "A홀", BigDecimal.TEN, BigDecimal.TEN, null);
        withId(hall, 10L);
        when(venueHallRepository.findByIdAndVenueId(10L, VENUE_ID)).thenReturn(Optional.of(hall));

        VenueHallResponse response =
                service.updateLayout(VENUE_ID, 10L, new UpdateVenueLayoutRequest(99L));

        assertThat(response.layoutFileId()).isEqualTo(99L);
    }
}
