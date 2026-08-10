package com.expo.booth.service;

import com.expo.booth.converter.BoothTemplateConverter;
import com.expo.booth.dto.BoothTemplateResponse;
import com.expo.booth.dto.CreateBoothTemplateRequest;
import com.expo.booth.entity.BoothTemplate;
import com.expo.booth.repository.BoothTemplateRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BoothTemplateService {

    private final BoothTemplateRepository boothTemplateRepository;
    private final BoothTemplateConverter boothTemplateConverter;

    public BoothTemplateService(
            BoothTemplateRepository boothTemplateRepository,
            BoothTemplateConverter boothTemplateConverter) {
        this.boothTemplateRepository = boothTemplateRepository;
        this.boothTemplateConverter = boothTemplateConverter;
    }

    /** 부스 템플릿 등록. 같은 형태 코드를 두 번 등록할 수 없다. */
    @Transactional
    public BoothTemplateResponse create(CreateBoothTemplateRequest request) {
        if (boothTemplateRepository.existsByShapeCode(request.shapeCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_BOOTH_TEMPLATE_SHAPE_CODE);
        }
        BoothTemplate template =
                BoothTemplate.create(
                        request.shapeCode(),
                        request.name(),
                        request.width(),
                        request.height(),
                        request.depth(),
                        request.dimensionUnit(),
                        request.defaultIncludedItems());
        try {
            BoothTemplate saved = boothTemplateRepository.saveAndFlush(template);
            return boothTemplateConverter.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("booth_templates_shape_code_key")) {
                throw new BusinessException(ErrorCode.DUPLICATE_BOOTH_TEMPLATE_SHAPE_CODE);
            }
            log.warn("부스 템플릿 저장 중 예상하지 못한 무결성 제약 위반. shapeCode={}", request.shapeCode(), e);
            throw e;
        }
    }

    /** 부스 템플릿 목록 조회. */
    @Transactional(readOnly = true)
    public List<BoothTemplateResponse> list() {
        return boothTemplateRepository.findAll().stream()
                .map(boothTemplateConverter::toResponse)
                .toList();
    }
}
