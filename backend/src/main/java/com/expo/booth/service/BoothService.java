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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Booth saved = boothRepository.save(booth);
        return boothConverter.toResponse(saved);
    }
}
