package com.expo.venue.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.venue.converter.VenueHallConverter;
import com.expo.venue.dto.CreateVenueHallRequest;
import com.expo.venue.dto.VenueHallResponse;
import com.expo.venue.entity.VenueHall;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VirtualVenueRepository;
import org.springframework.dao.DataIntegrityViolationException;
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

    /**
     * 장소 안에 홀 등록. 같은 장소 안에서 홀 코드가 중복될 수 없다.
     *
     * <p>킨텍스 실제 구조(제1·제2전시장)에 맞춰 한 장소에는 홀을 2개까지만 등록할 수 있다.
     *
     * <p>사전 검사만으로는 동시 요청 사이의 중복·초과 삽입을 막지 못해, DB의 {@code uq_venue_halls_code} 고유 제약과
     * {@code trg_venue_hall_limit} 트리거를 최종 방어선으로 삼는다.
     */
    @Transactional
    public VenueHallResponse create(Long venueId, CreateVenueHallRequest request) {
        if (!virtualVenueRepository.existsById(venueId)) {
            throw new BusinessException(ErrorCode.VIRTUAL_VENUE_NOT_FOUND);
        }
        if (venueHallRepository.existsByVenueIdAndHallCode(venueId, request.hallCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_VENUE_HALL_CODE);
        }
        if (venueHallRepository.countByVenueId(venueId) >= 2) {
            throw new BusinessException(ErrorCode.VENUE_HALL_LIMIT_EXCEEDED);
        }
        VenueHall hall =
                VenueHall.create(
                        venueId,
                        request.hallCode(),
                        request.name(),
                        request.width(),
                        request.depth(),
                        request.layoutFileId());
        try {
            VenueHall saved = venueHallRepository.saveAndFlush(hall);
            return venueHallConverter.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("uq_venue_halls_code")) {
                throw new BusinessException(ErrorCode.DUPLICATE_VENUE_HALL_CODE);
            }
            if (cause != null && cause.contains("venue_hall_limit_exceeded")) {
                throw new BusinessException(ErrorCode.VENUE_HALL_LIMIT_EXCEEDED);
            }
            throw e;
        }
    }
}
