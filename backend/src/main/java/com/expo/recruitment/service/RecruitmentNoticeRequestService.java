package com.expo.recruitment.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.recruitment.converter.RecruitmentNoticeRequestConverter;
import com.expo.recruitment.dto.CreateRecruitmentNoticeRequestRequest;
import com.expo.recruitment.dto.RecruitmentNoticeRequestResponse;
import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import com.expo.recruitment.repository.RecruitmentNoticeRequestRepository;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VenueZoneRepository;
import com.expo.venue.repository.VirtualVenueRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruitmentNoticeRequestService {

    private final RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository;
    private final VirtualVenueRepository virtualVenueRepository;
    private final VenueHallRepository venueHallRepository;
    private final VenueZoneRepository venueZoneRepository;
    private final RecruitmentNoticeRequestConverter recruitmentNoticeRequestConverter;

    public RecruitmentNoticeRequestService(
            RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository,
            VirtualVenueRepository virtualVenueRepository,
            VenueHallRepository venueHallRepository,
            VenueZoneRepository venueZoneRepository,
            RecruitmentNoticeRequestConverter recruitmentNoticeRequestConverter) {
        this.recruitmentNoticeRequestRepository = recruitmentNoticeRequestRepository;
        this.virtualVenueRepository = virtualVenueRepository;
        this.venueHallRepository = venueHallRepository;
        this.venueZoneRepository = venueZoneRepository;
        this.recruitmentNoticeRequestConverter = recruitmentNoticeRequestConverter;
    }

    /** 모집공고 생성 요청 작성. 희망 장소가 실제로 존재하는지 검증한다. */
    @Transactional
    public RecruitmentNoticeRequestResponse create(
            Long hostClientId, CreateRecruitmentNoticeRequestRequest request) {
        if (!virtualVenueRepository.existsById(request.virtualVenueId())) {
            throw new BusinessException(ErrorCode.VIRTUAL_VENUE_NOT_FOUND);
        }
        if (request.venueHallId() != null
                && !venueHallRepository.existsById(request.venueHallId())) {
            throw new BusinessException(ErrorCode.VENUE_HALL_NOT_FOUND);
        }
        if (request.venueZoneId() != null
                && !venueZoneRepository.existsById(request.venueZoneId())) {
            throw new BusinessException(ErrorCode.VENUE_ZONE_NOT_FOUND);
        }
        RecruitmentNoticeRequest entity =
                RecruitmentNoticeRequest.create(
                                hostClientId,
                                request.title(),
                                request.description(),
                                request.applicationStartAt(),
                                request.applicationEndAt(),
                                request.eventStartAt(),
                                request.eventEndAt(),
                                request.virtualVenueId())
                        .withVenueDetails(
                                request.venueHallId(),
                                request.venueZoneId(),
                                request.targetCompanyCount(),
                                request.requestedBoothConfig());
        RecruitmentNoticeRequest saved = recruitmentNoticeRequestRepository.save(entity);
        return recruitmentNoticeRequestConverter.toResponse(saved);
    }

    /** 주최 클라이언트 본인이 작성한 모집공고 생성 요청 목록 조회. */
    @Transactional(readOnly = true)
    public List<RecruitmentNoticeRequestResponse> listMine(Long hostClientId) {
        return recruitmentNoticeRequestRepository.findAllByHostClientId(hostClientId).stream()
                .map(recruitmentNoticeRequestConverter::toResponse)
                .toList();
    }

    /** 주최 클라이언트 본인이 작성한 모집공고 생성 요청 상세 조회. */
    @Transactional(readOnly = true)
    public RecruitmentNoticeRequestResponse getMine(Long requestId, Long hostClientId) {
        RecruitmentNoticeRequest request =
                recruitmentNoticeRequestRepository
                        .findByIdAndHostClientId(requestId, hostClientId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.RECRUITMENT_NOTICE_REQUEST_NOT_FOUND));
        return recruitmentNoticeRequestConverter.toResponse(request);
    }

    /** 관리자용 모집공고 생성 요청 목록 조회. */
    @Transactional(readOnly = true)
    public List<RecruitmentNoticeRequestResponse> listForAdmin() {
        return recruitmentNoticeRequestRepository.findAll().stream()
                .map(recruitmentNoticeRequestConverter::toResponse)
                .toList();
    }

    /** 관리자용 모집공고 생성 요청 상세 조회. */
    @Transactional(readOnly = true)
    public RecruitmentNoticeRequestResponse getForAdmin(Long requestId) {
        RecruitmentNoticeRequest request =
                recruitmentNoticeRequestRepository
                        .findById(requestId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.RECRUITMENT_NOTICE_REQUEST_NOT_FOUND));
        return recruitmentNoticeRequestConverter.toResponse(request);
    }
}
