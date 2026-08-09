package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.booth.converter.BoothConverter;
import com.expo.booth.dto.BoothResponse;
import com.expo.booth.dto.CreateBoothRequest;
import com.expo.booth.repository.BoothRepository;
import com.expo.booth.repository.BoothTemplateRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.venue.repository.VenueZoneRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        verify(boothRepository, never()).save(any());
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
        when(boothRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BoothResponse response = service.create(ZONE_ID, requestWithTemplate(null));

        assertThat(response.venueZoneId()).isEqualTo(ZONE_ID);
        assertThat(response.boothNumber()).isEqualTo("A-01");
    }

    @Test
    void createSucceedsWithValidTemplate() {
        when(venueZoneRepository.existsById(ZONE_ID)).thenReturn(true);
        when(boothTemplateRepository.existsById(TEMPLATE_ID)).thenReturn(true);
        when(boothRepository.existsByVenueZoneIdAndBoothNumber(ZONE_ID, "A-01")).thenReturn(false);
        when(boothRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BoothResponse response = service.create(ZONE_ID, requestWithTemplate(TEMPLATE_ID));

        assertThat(response.boothTemplateId()).isEqualTo(TEMPLATE_ID);
    }
}
