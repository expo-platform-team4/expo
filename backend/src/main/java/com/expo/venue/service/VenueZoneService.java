package com.expo.venue.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.venue.converter.VenueZoneConverter;
import com.expo.venue.dto.CreateVenueZoneRequest;
import com.expo.venue.dto.VenueZoneResponse;
import com.expo.venue.entity.VenueZone;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VenueZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VenueZoneService {

    private final VenueZoneRepository venueZoneRepository;
    private final VenueHallRepository venueHallRepository;
    private final VenueZoneConverter venueZoneConverter;

    public VenueZoneService(
            VenueZoneRepository venueZoneRepository,
            VenueHallRepository venueHallRepository,
            VenueZoneConverter venueZoneConverter) {
        this.venueZoneRepository = venueZoneRepository;
        this.venueHallRepository = venueHallRepository;
        this.venueZoneConverter = venueZoneConverter;
    }

    /** 홀 안에 구역 등록. 같은 홀 안에서 구역 코드가 중복될 수 없다. */
    @Transactional
    public VenueZoneResponse create(Long hallId, CreateVenueZoneRequest request) {
        if (!venueHallRepository.existsById(hallId)) {
            throw new BusinessException(ErrorCode.VENUE_HALL_NOT_FOUND);
        }
        if (venueZoneRepository.existsByHallIdAndZoneCode(hallId, request.zoneCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_VENUE_ZONE_CODE);
        }
        VenueZone zone =
                VenueZone.create(
                        hallId,
                        request.zoneCode(),
                        request.name(),
                        request.maxBoothCount(),
                        request.width(),
                        request.depth(),
                        request.layoutFileId());
        VenueZone saved = venueZoneRepository.save(zone);
        return venueZoneConverter.toResponse(saved);
    }
}
