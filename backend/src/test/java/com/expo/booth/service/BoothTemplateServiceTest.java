package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.booth.converter.BoothTemplateConverter;
import com.expo.booth.dto.BoothTemplateResponse;
import com.expo.booth.dto.CreateBoothTemplateRequest;
import com.expo.booth.repository.BoothTemplateRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link BoothTemplateService} 의 형태 코드 중복 검증을 확인한다. */
class BoothTemplateServiceTest {

    private BoothTemplateRepository boothTemplateRepository;
    private BoothTemplateService service;

    @BeforeEach
    void setUp() {
        boothTemplateRepository = mock(BoothTemplateRepository.class);
        service = new BoothTemplateService(boothTemplateRepository, new BoothTemplateConverter());
    }

    private CreateBoothTemplateRequest request() {
        return new CreateBoothTemplateRequest(
                "STANDARD-3X3",
                "기본 3x3 부스",
                BigDecimal.valueOf(3),
                null,
                BigDecimal.valueOf(3),
                null,
                null);
    }

    @Test
    void createRejectsDuplicateShapeCode() {
        when(boothTemplateRepository.existsByShapeCode("STANDARD-3X3")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_BOOTH_TEMPLATE_SHAPE_CODE);
        verify(boothTemplateRepository, never()).save(any());
    }

    @Test
    void createSucceeds() {
        when(boothTemplateRepository.existsByShapeCode("STANDARD-3X3")).thenReturn(false);
        when(boothTemplateRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BoothTemplateResponse response = service.create(request());

        assertThat(response.shapeCode()).isEqualTo("STANDARD-3X3");
        assertThat(response.name()).isEqualTo("기본 3x3 부스");
    }
}
