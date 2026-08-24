package com.expo.venue.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.venue.entity.VenueZone;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VenueZoneRepository;
import com.expo.venue.repository.VirtualVenueRepository;
import org.springframework.stereotype.Component;

/** 장소·홀·구역 계층 소속 검증. */
@Component
public class VenueHierarchyValidator {

    private final VirtualVenueRepository virtualVenueRepository;
    private final VenueHallRepository venueHallRepository;
    private final VenueZoneRepository venueZoneRepository;

    public VenueHierarchyValidator(
            VirtualVenueRepository virtualVenueRepository,
            VenueHallRepository venueHallRepository,
            VenueZoneRepository venueZoneRepository) {
        this.virtualVenueRepository = virtualVenueRepository;
        this.venueHallRepository = venueHallRepository;
        this.venueZoneRepository = venueZoneRepository;
    }

    /**
     * 홀·구역이 지정한 장소·홀 소속인지 검증하고, 실제로 사용할 홀 ID를 반환한다.
     *
     * <p>구역만 지정하고 홀을 안 넘긴 경우, 구역이 속한 홀을 조회해서 검증 기준이자 반환값으로 삼는다. 저장되는 예약에도 이
     * 반환값을 써야 한다 — {@code venue_zone_id} 를 지정하면 {@code venue_hall_id} 도 반드시 있어야 한다는 DB 제약을
     * 만족시키기 위함이다.
     */
    public Long validate(Long virtualVenueId, Long venueHallId, Long venueZoneId) {
        if (!virtualVenueRepository.existsById(virtualVenueId)) {
            throw new BusinessException(ErrorCode.VIRTUAL_VENUE_NOT_FOUND);
        }
        Long effectiveHallId = venueHallId;
        if (venueZoneId != null) {
            VenueZone zone =
                    venueZoneRepository
                            .findById(venueZoneId)
                            .orElseThrow(
                                    () -> new BusinessException(ErrorCode.VENUE_ZONE_NOT_FOUND));
            if (venueHallId != null && !zone.getHallId().equals(venueHallId)) {
                throw new BusinessException(ErrorCode.VENUE_HALL_ZONE_MISMATCH);
            }
            effectiveHallId = zone.getHallId();
        }
        if (effectiveHallId != null) {
            if (!venueHallRepository.existsById(effectiveHallId)) {
                throw new BusinessException(ErrorCode.VENUE_HALL_NOT_FOUND);
            }
            if (!venueHallRepository.existsByIdAndVenueId(effectiveHallId, virtualVenueId)) {
                throw new BusinessException(ErrorCode.VENUE_HALL_ZONE_MISMATCH);
            }
        }
        return effectiveHallId;
    }
}
