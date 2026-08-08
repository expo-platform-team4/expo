package com.expo.venue.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.venue.converter.VenueHallConverter;
import com.expo.venue.dto.CreateVenueHallRequest;
import com.expo.venue.dto.VenueHallResponse;
import com.expo.venue.entity.VenueHall;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VirtualVenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VenueHallService {

    private final VenueHallRepository venueHallRepository;
    private final VirtualVenueRepository virtualVenueRepository;
    private final VenueHallConverter venueHallConverter;

    public VenueHallService(
            VenueHallRepository venueHallRepository,
            VirtualVenueRepository virtualVenueRepository,
            VenueHallConverter venueHallConverter) {
        this.venueHallRepository = venueHallRepository;
        this.virtualVenueRepository = virtualVenueRepository;
        this.venueHallConverter = venueHallConverter;
    }

    /** 장소 안에 홀 등록. 같은 장소 안에서 홀 코드가 중복될 수 없다. */
    @Transactional
    public VenueHallResponse create(Long venueId, CreateVenueHallRequest request) {
        if (!virtualVenueRepository.existsById(venueId)) {
            throw new BusinessException(ErrorCode.VIRTUAL_VENUE_NOT_FOUND);
        }
        if (venueHallRepository.existsByVenueIdAndHallCode(venueId, request.hallCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_VENUE_HALL_CODE);
        }
        VenueHall hall =
                VenueHall.create(
                        venueId,
                        request.hallCode(),
                        request.name(),
                        request.width(),
                        request.depth(),
                        request.layoutFileId());
        VenueHall saved = venueHallRepository.save(hall);
        return venueHallConverter.toResponse(saved);
    }
}
