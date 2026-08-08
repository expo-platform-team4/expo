package com.expo.booth.service;

import com.expo.booth.converter.BoothTemplateConverter;
import com.expo.booth.dto.BoothTemplateResponse;
import com.expo.booth.dto.CreateBoothTemplateRequest;
import com.expo.booth.entity.BoothTemplate;
import com.expo.booth.repository.BoothTemplateRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        BoothTemplate saved = boothTemplateRepository.save(template);
        return boothTemplateConverter.toResponse(saved);
    }

    /** 부스 템플릿 목록 조회. */
    @Transactional(readOnly = true)
    public List<BoothTemplateResponse> list() {
        return boothTemplateRepository.findAll().stream()
                .map(boothTemplateConverter::toResponse)
                .toList();
    }
}
