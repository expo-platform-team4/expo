package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.booth.converter.BoothConverter;
import com.expo.booth.dto.BoothResponse;
import com.expo.booth.dto.CreateBoothRequest;
import com.expo.booth.entity.Booth;
import com.expo.booth.repository.BoothRepository;
import com.expo.booth.repository.BoothTemplateRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.venue.repository.VenueZoneRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** {@link BoothService} 의 구역 존재 여부·템플릿 존재 여부·부스 번호 중복 검증을 확인한다. */
class BoothServiceTest {

    private static final Long ZONE_ID = 1L;
    private static final Long TEMPLATE_ID = 2L;

    private BoothRepository boothRepository;
    private VenueZoneRepository venueZoneRepository;
    private BoothTemplateRepository boothTemplateRepository;
    private BoothService service;

    @BeforeEach
    void setUp() {
        boothRepository = mock(BoothRepository.class);
        venueZoneRepository = mock(VenueZoneRepository.class);
        boothTemplateRepository = mock(BoothTemplateRepository.class);
        service =
                new BoothService(
                        boothRepository,
                        venueZoneRepository,
                        boothTemplateRepository,
                        new BoothConverter());
    }

    private CreateBoothRequest requestWithTemplate(Long templateId) {
        return new CreateBoothRequest(
                templateId,
                "A-01",
                "STANDARD-3X3",
                BigDecimal.valueOf(3),
                null,
                BigDecimal.valueOf(3),
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void createRejectsWhenZoneNotFound() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(ZONE_ID, requestWithTemplate(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_ZONE_NOT_FOUND);
        verify(boothRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsWhenBoothTemplateNotFound() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(true);
        when(boothTemplateRepository.existsById(TEMPLATE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(ZONE_ID, requestWithTemplate(TEMPLATE_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_TEMPLATE_NOT_FOUND);
    }

    @Test
    void createRejectsDuplicateBoothNumber() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(true);
        when(boothRepository.existsByVenueZoneIdAndBoothNumber(ZONE_ID, "A-01")).thenReturn(true);

        assertThatThrownBy(() -> service.create(ZONE_ID, requestWithTemplate(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_BOOTH_NUMBER);
    }

    @Test
    void createSucceedsWithoutTemplate() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(true);
        when(boothRepository.existsByVenueZoneIdAndBoothNumber(ZONE_ID, "A-01")).thenReturn(false);
        when(boothRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BoothResponse response = service.create(ZONE_ID, requestWithTemplate(null));

        assertThat(response.venueZoneId()).isEqualTo(ZONE_ID);
        assertThat(response.boothNumber()).isEqualTo("A-01");
        verify(boothRepository).saveAndFlush(any());
    }

    @Test
    void createSucceedsWithValidTemplate() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(true);
        when(boothTemplateRepository.existsById(TEMPLATE_ID)).thenReturn(true);
        when(boothRepository.existsByVenueZoneIdAndBoothNumber(ZONE_ID, "A-01")).thenReturn(false);
        when(boothRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BoothResponse response = service.create(ZONE_ID, requestWithTemplate(TEMPLATE_ID));

        assertThat(response.boothTemplateId()).isEqualTo(TEMPLATE_ID);
    }

    /** 사전 중복 검사를 통과해도 동시 삽입으로 제약 위반이 나면 같은 오류로 변환돼야 한다. */
    @Test
    void createTranslatesBoothNumberConstraintViolationToDuplicate() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(true);
        when(boothRepository.existsByVenueZoneIdAndBoothNumber(ZONE_ID, "A-01")).thenReturn(false);
        when(boothRepository.saveAndFlush(any()))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "duplicate key value violates unique constraint"
                                        + " \"uq_booths_number\""));

        assertThatThrownBy(() -> service.create(ZONE_ID, requestWithTemplate(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_BOOTH_NUMBER);
    }

    /** 부스 번호 중복이 아닌 다른 무결성 위반은 그대로 다시 던져야 한다. */
    @Test
    void createRethrowsUnrelatedConstraintViolation() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(true);
        when(boothRepository.existsByVenueZoneIdAndBoothNumber(ZONE_ID, "A-01")).thenReturn(false);
        when(boothRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("some other constraint"));

        assertThatThrownBy(() -> service.create(ZONE_ID, requestWithTemplate(null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private CreateBoothRequest requestWithNumber(String boothNumber) {
        return new CreateBoothRequest(
                null,
                boothNumber,
                "STANDARD-3X3",
                BigDecimal.valueOf(3),
                null,
                BigDecimal.valueOf(3),
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void createBulkRejectsWhenZoneNotFound() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.createBulk(ZONE_ID, List.of(requestWithNumber("A-01"))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_ZONE_NOT_FOUND);
        verify(boothRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void createBulkRejectsDuplicateBoothNumberWithinBatch() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(true);
        when(boothRepository.existsByVenueZoneIdAndBoothNumber(ZONE_ID, "A-01")).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.createBulk(
                                        ZONE_ID,
                                        List.of(
                                                requestWithNumber("A-01"),
                                                requestWithNumber("A-01"))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_BOOTH_NUMBER);
        verify(boothRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void createBulkRejectsWhenAnyBoothNumberAlreadyExists() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(true);
        when(boothRepository.existsByVenueZoneIdAndBoothNumber(ZONE_ID, "A-01")).thenReturn(false);
        when(boothRepository.existsByVenueZoneIdAndBoothNumber(ZONE_ID, "A-02")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.createBulk(
                                        ZONE_ID,
                                        List.of(
                                                requestWithNumber("A-01"),
                                                requestWithNumber("A-02"))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_BOOTH_NUMBER);
        verify(boothRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void createBulkSucceeds() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(true);
        when(boothRepository.existsByVenueZoneIdAndBoothNumber(eq(ZONE_ID), any()))
                .thenReturn(false);
        when(boothRepository.saveAllAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<BoothResponse> responses =
                service.createBulk(
                        ZONE_ID, List.of(requestWithNumber("A-01"), requestWithNumber("A-02")));

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(BoothResponse::boothNumber)
                .containsExactly("A-01", "A-02");
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
    void listRejectsWhenZoneNotFound() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.list(ZONE_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_ZONE_NOT_FOUND);
    }

    @Test
    void listReturnsBoothsInZone() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(true);
        Booth booth =
                Booth.create(
                        ZONE_ID,
                        null,
                        "A-01",
                        "STANDARD-3X3",
                        BigDecimal.valueOf(3),
                        null,
                        BigDecimal.valueOf(3),
                        null);
        withId(booth, 10L);
        when(boothRepository.findAllByVenueZoneIdOrderBySortOrderAscIdAsc(ZONE_ID))
                .thenReturn(List.of(booth));

        List<BoothResponse> responses = service.list(ZONE_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(10L);
    }

    @Test
    void getRejectsWhenZoneNotFound() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.get(ZONE_ID, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_ZONE_NOT_FOUND);
        verify(boothRepository, never()).findByIdAndVenueZoneId(any(), any());
    }

    @Test
    void getRejectsWhenNotFoundInZone() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(true);
        when(boothRepository.findByIdAndVenueZoneId(10L, ZONE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(ZONE_ID, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_NOT_FOUND);
    }

    @Test
    void getSucceeds() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(true);
        Booth booth =
                Booth.create(
                        ZONE_ID,
                        null,
                        "A-01",
                        "STANDARD-3X3",
                        BigDecimal.valueOf(3),
                        null,
                        BigDecimal.valueOf(3),
                        null);
        withId(booth, 10L);
        when(boothRepository.findByIdAndVenueZoneId(10L, ZONE_ID)).thenReturn(Optional.of(booth));

        BoothResponse response = service.get(ZONE_ID, 10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.boothNumber()).isEqualTo("A-01");
    }
}
