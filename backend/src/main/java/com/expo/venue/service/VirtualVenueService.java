package com.expo.venue.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.venue.converter.VirtualVenueConverter;
import com.expo.venue.dto.CreateVirtualVenueRequest;
import com.expo.venue.dto.VirtualVenueResponse;
import com.expo.venue.entity.VirtualVenue;
import com.expo.venue.repository.VirtualVenueRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VirtualVenueService {

    private final VirtualVenueRepository virtualVenueRepository;
    private final VirtualVenueConverter virtualVenueConverter;

    public VirtualVenueService(
            VirtualVenueRepository virtualVenueRepository,
            VirtualVenueConverter virtualVenueConverter) {
        this.virtualVenueRepository = virtualVenueRepository;
        this.virtualVenueConverter = virtualVenueConverter;
    }

    /**
     * 가상 장소 등록. 같은 이름의 장소를 두 번 등록할 수 없다.
     *
     * <p>사전 중복 검사만으로는 동시 요청 사이의 중복 삽입을 막지 못해, DB의 {@code virtual_venues_name_key} 고유 제약을
     * 최종 방어선으로 삼는다.
     */
    @Transactional
    public VirtualVenueResponse create(CreateVirtualVenueRequest request) {
        if (virtualVenueRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.DUPLICATE_VIRTUAL_VENUE_NAME);
        }
        VirtualVenue venue =
                VirtualVenue.create(
                        request.name(),
                        request.address(),
                        request.regionCode(),
                        request.description(),
                        request.mapFileId());
        try {
            VirtualVenue saved = virtualVenueRepository.saveAndFlush(venue);
            return virtualVenueConverter.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("virtual_venues_name_key")) {
                throw new BusinessException(ErrorCode.DUPLICATE_VIRTUAL_VENUE_NAME);
            }
            throw e;
        }
    }

    /** 가상 장소 목록 조회. */
    @Transactional(readOnly = true)
    public List<VirtualVenueResponse> list() {
        return virtualVenueRepository.findAll().stream()
                .map(virtualVenueConverter::toResponse)
                .toList();
    }
}
