package com.expo.booth.service;

import com.expo.booth.converter.BoothConverter;
import com.expo.booth.dto.BoothResponse;
import com.expo.booth.dto.CreateBoothRequest;
import com.expo.booth.entity.Booth;
import com.expo.booth.repository.BoothRepository;
import com.expo.booth.repository.BoothTemplateRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.venue.repository.VenueZoneRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BoothService {

    private final BoothRepository boothRepository;
    private final VenueZoneRepository venueZoneRepository;
    private final BoothTemplateRepository boothTemplateRepository;
    private final BoothConverter boothConverter;

    public BoothService(
            BoothRepository boothRepository,
            VenueZoneRepository venueZoneRepository,
            BoothTemplateRepository boothTemplateRepository,
            BoothConverter boothConverter) {
        this.boothRepository = boothRepository;
        this.venueZoneRepository = venueZoneRepository;
        this.boothTemplateRepository = boothTemplateRepository;
        this.boothConverter = boothConverter;
    }

    /** 구역 안에 부스 공간 등록. 같은 구역 안에서 부스 번호가 중복될 수 없다. */
    @Transactional
    public BoothResponse create(Long venueZoneId, CreateBoothRequest request) {
        if (!venueZoneRepository.existsById(venueZoneId)) {
            throw new BusinessException(ErrorCode.VENUE_ZONE_NOT_FOUND);
        }
        if (request.boothTemplateId() != null
                && !boothTemplateRepository.existsById(request.boothTemplateId())) {
            throw new BusinessException(ErrorCode.BOOTH_TEMPLATE_NOT_FOUND);
        }
        if (boothRepository.existsByVenueZoneIdAndBoothNumber(venueZoneId, request.boothNumber())) {
            throw new BusinessException(ErrorCode.DUPLICATE_BOOTH_NUMBER);
        }
        Booth booth =
                Booth.create(
                                venueZoneId,
                                request.boothTemplateId(),
                                request.boothNumber(),
                                request.shapeCode(),
                                request.width(),
                                request.height(),
                                request.depth(),
                                request.dimensionUnit())
                        .place(
                                request.positionX(),
                                request.positionY(),
                                request.rotationDegree(),
                                request.sortOrder());
        try {
            Booth saved = boothRepository.saveAndFlush(booth);
            return boothConverter.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("uq_booths_number")) {
                throw new BusinessException(ErrorCode.DUPLICATE_BOOTH_NUMBER);
            }
            log.warn(
                    "부스 저장 중 예상하지 못한 무결성 제약 위반. venueZoneId={}, boothNumber={}",
                    venueZoneId,
                    request.boothNumber(),
                    e);
            throw e;
        }
    }

    /** 구역 내 부스 공간 목록 조회. 도면 정렬 순서대로 반환한다. */
    @Transactional(readOnly = true)
    public List<BoothResponse> list(Long venueZoneId) {
        if (!venueZoneRepository.existsById(venueZoneId)) {
            throw new BusinessException(ErrorCode.VENUE_ZONE_NOT_FOUND);
        }
        return boothRepository.findAllByVenueZoneIdOrderBySortOrderAscIdAsc(venueZoneId).stream()
                .map(boothConverter::toResponse)
                .toList();
    }

    /** 구역 내 부스 공간 상세 조회. */
    @Transactional(readOnly = true)
    public BoothResponse get(Long venueZoneId, Long boothId) {
        Booth booth =
                boothRepository
                        .findByIdAndVenueZoneId(boothId, venueZoneId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_NOT_FOUND));
        return boothConverter.toResponse(booth);
    }
}
