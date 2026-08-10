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
import org.springframework.dao.DataIntegrityViolationException;

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
        verify(boothTemplateRepository, never()).saveAndFlush(any());
    }

    @Test
    void createSucceeds() {
        when(boothTemplateRepository.existsByShapeCode("STANDARD-3X3")).thenReturn(false);
        when(boothTemplateRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BoothTemplateResponse response = service.create(request());

        assertThat(response.shapeCode()).isEqualTo("STANDARD-3X3");
        assertThat(response.name()).isEqualTo("기본 3x3 부스");
        verify(boothTemplateRepository).saveAndFlush(any());
    }

    /** 사전 중복 검사를 통과해도 동시 삽입으로 제약 위반이 나면 같은 오류로 변환돼야 한다. */
    @Test
    void createTranslatesShapeCodeConstraintViolationToDuplicate() {
        when(boothTemplateRepository.existsByShapeCode("STANDARD-3X3")).thenReturn(false);
        when(boothTemplateRepository.saveAndFlush(any()))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "duplicate key value violates unique constraint"
                                        + " \"booth_templates_shape_code_key\""));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_BOOTH_TEMPLATE_SHAPE_CODE);
    }

    /** 형태 코드 중복이 아닌 다른 무결성 위반은 그대로 다시 던져야 한다. */
    @Test
    void createRethrowsUnrelatedConstraintViolation() {
        when(boothTemplateRepository.existsByShapeCode("STANDARD-3X3")).thenReturn(false);
        when(boothTemplateRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("some other constraint"));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
