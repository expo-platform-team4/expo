package com.expo.venue.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.venue.converter.VenueZoneConverter;
import com.expo.venue.dto.CreateVenueZoneRequest;
import com.expo.venue.dto.VenueZoneResponse;
import com.expo.venue.entity.VenueZone;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VenueZoneRepository;
import org.springframework.dao.DataIntegrityViolationException;
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

    /**
     * 홀 안에 구역 등록. 같은 홀 안에서 구역 코드가 중복될 수 없다.
     *
     * <p>킨텍스 실제 구조(전시장마다 1~5홀)에 맞춰 한 홀에는 구역을 5개까지만 등록할 수 있다.
     *
     * <p>사전 검사만으로는 동시 요청 사이의 중복·초과 삽입을 막지 못해, DB의 {@code uq_venue_zones_code} 고유 제약과
     * {@code trg_venue_zone_limit} 트리거를 최종 방어선으로 삼는다.
     */
    @Transactional
    public VenueZoneResponse create(Long hallId, CreateVenueZoneRequest request) {
        if (!venueHallRepository.existsById(hallId)) {
            throw new BusinessException(ErrorCode.VENUE_HALL_NOT_FOUND);
        }
        if (venueZoneRepository.existsByHallIdAndZoneCode(hallId, request.zoneCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_VENUE_ZONE_CODE);
        }
        if (venueZoneRepository.countByHallId(hallId) >= 5) {
            throw new BusinessException(ErrorCode.VENUE_ZONE_LIMIT_EXCEEDED);
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
        try {
            VenueZone saved = venueZoneRepository.saveAndFlush(zone);
            return venueZoneConverter.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("uq_venue_zones_code")) {
                throw new BusinessException(ErrorCode.DUPLICATE_VENUE_ZONE_CODE);
            }
            if (cause != null && cause.contains("venue_zone_limit_exceeded")) {
                throw new BusinessException(ErrorCode.VENUE_ZONE_LIMIT_EXCEEDED);
            }
            throw e;
        }
    }
}
