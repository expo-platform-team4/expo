package com.expo.recruitment.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.recruitment.converter.RecruitmentNoticeRequestConverter;
import com.expo.recruitment.dto.CreateRecruitmentNoticeRequestRequest;
import com.expo.recruitment.dto.DecideVenueRequest;
import com.expo.recruitment.dto.RecruitmentNoticeRequestResponse;
import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import com.expo.recruitment.entity.RecruitmentNoticeRequestActionType;
import com.expo.recruitment.entity.RecruitmentNoticeRequestHistory;
import com.expo.recruitment.entity.RecruitmentNoticeRequestZone;
import com.expo.recruitment.entity.VenueDecision;
import com.expo.recruitment.repository.RecruitmentNoticeRequestHistoryRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRequestRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRequestZoneRepository;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VenueZoneRepository;
import com.expo.venue.repository.VirtualVenueRepository;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruitmentNoticeRequestService {

    private final RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository;
    private final RecruitmentNoticeRequestZoneRepository recruitmentNoticeRequestZoneRepository;
    private final RecruitmentNoticeRequestHistoryRepository
            recruitmentNoticeRequestHistoryRepository;
    private final VirtualVenueRepository virtualVenueRepository;
    private final VenueHallRepository venueHallRepository;
    private final VenueZoneRepository venueZoneRepository;
    private final RecruitmentNoticeRequestConverter recruitmentNoticeRequestConverter;

    public RecruitmentNoticeRequestService(
            RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository,
            RecruitmentNoticeRequestZoneRepository recruitmentNoticeRequestZoneRepository,
            RecruitmentNoticeRequestHistoryRepository recruitmentNoticeRequestHistoryRepository,
            VirtualVenueRepository virtualVenueRepository,
            VenueHallRepository venueHallRepository,
            VenueZoneRepository venueZoneRepository,
            RecruitmentNoticeRequestConverter recruitmentNoticeRequestConverter) {
        this.recruitmentNoticeRequestRepository = recruitmentNoticeRequestRepository;
        this.recruitmentNoticeRequestZoneRepository = recruitmentNoticeRequestZoneRepository;
        this.recruitmentNoticeRequestHistoryRepository = recruitmentNoticeRequestHistoryRepository;
        this.virtualVenueRepository = virtualVenueRepository;
        this.venueHallRepository = venueHallRepository;
        this.venueZoneRepository = venueZoneRepository;
        this.recruitmentNoticeRequestConverter = recruitmentNoticeRequestConverter;
    }

    /**
     * 모집공고 생성 요청 작성. 희망 전시관(홀)이 실제로 존재하는지, 고른 구역이 전부 그 전시관 소속인지, 기간 순서가 올바른지
     * 검증한다.
     *
     * <p>고른 구역이 전시관 소속인지는 서비스에서도 먼저 확인하지만, DB의 복합 FK({@code
     * fk_notice_request_zones_zone_in_hall})가 최종 방어선이다.
     */
    @Transactional
    public RecruitmentNoticeRequestResponse create(
            Long hostClientId, CreateRecruitmentNoticeRequestRequest request) {
        if (!request.applicationEndAt().isAfter(request.applicationStartAt())) {
            throw new BusinessException(ErrorCode.APPLICATION_PERIOD_INVALID);
        }
        if (!request.eventEndAt().isAfter(request.eventStartAt())) {
            throw new BusinessException(ErrorCode.EVENT_PERIOD_INVALID);
        }
        if (!virtualVenueRepository.existsById(request.virtualVenueId())) {
            throw new BusinessException(ErrorCode.VIRTUAL_VENUE_NOT_FOUND);
        }
        if (!venueHallRepository.existsById(request.venueHallId())) {
            throw new BusinessException(ErrorCode.VENUE_HALL_NOT_FOUND);
        }
        if (!venueHallRepository.existsByIdAndVenueId(
                request.venueHallId(), request.virtualVenueId())) {
            throw new BusinessException(ErrorCode.VENUE_HALL_ZONE_MISMATCH);
        }
        List<Long> venueZoneIds = List.copyOf(new LinkedHashSet<>(request.venueZoneIds()));
        for (Long zoneId : venueZoneIds) {
            if (!venueZoneRepository.existsByIdAndHallId(zoneId, request.venueHallId())) {
                throw new BusinessException(ErrorCode.VENUE_HALL_ZONE_MISMATCH);
            }
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
                                request.targetCompanyCount(),
                                request.requestedBoothConfig());
        RecruitmentNoticeRequest saved = recruitmentNoticeRequestRepository.save(entity);
        for (Long zoneId : venueZoneIds) {
            recruitmentNoticeRequestZoneRepository.save(
                    RecruitmentNoticeRequestZone.create(
                            saved.getId(), request.venueHallId(), zoneId));
        }
        return recruitmentNoticeRequestConverter.toResponse(saved, venueZoneIds);
    }

    /** 주최 클라이언트 본인이 작성한 모집공고 생성 요청 목록 조회. */
    @Transactional(readOnly = true)
    public List<RecruitmentNoticeRequestResponse> listMine(Long hostClientId) {
        return recruitmentNoticeRequestRepository.findAllByHostClientId(hostClientId).stream()
                .map(this::toResponseWithZones)
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
        return toResponseWithZones(request);
    }

    /** 관리자용 모집공고 생성 요청 목록 조회. */
    @Transactional(readOnly = true)
    public List<RecruitmentNoticeRequestResponse> listForAdmin() {
        return recruitmentNoticeRequestRepository.findAll().stream()
                .map(this::toResponseWithZones)
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
        return toResponseWithZones(request);
    }

    /**
     * 장소 충돌 판정. ALLOWED 또는 CANCELED 만 허용하며, 이미 결정된 요청은 다시 판정할 수 없다.
     *
     * <p>권한이 걸린 변경이라 변경 전후 상태를 감사 이력({@code recruitment_notice_request_histories})에
     * 남긴다.
     */
    @Transactional
    public RecruitmentNoticeRequestResponse decideVenue(
            Long requestId, Long adminId, DecideVenueRequest request) {
        if (request.decision() != VenueDecision.ALLOWED
                && request.decision() != VenueDecision.CANCELED) {
            throw new BusinessException(ErrorCode.VENUE_DECISION_INVALID);
        }
        RecruitmentNoticeRequest entity =
                recruitmentNoticeRequestRepository
                        .findByIdForUpdate(requestId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.RECRUITMENT_NOTICE_REQUEST_NOT_FOUND));
        if (entity.getVenueDecision() != VenueDecision.PENDING) {
            throw new BusinessException(ErrorCode.VENUE_DECISION_ALREADY_MADE);
        }
        VenueDecision previousDecision = entity.getVenueDecision();
        entity.decideVenue(request.decision(), adminId, request.reason());
        recruitmentNoticeRequestHistoryRepository.save(
                RecruitmentNoticeRequestHistory.create(
                        entity.getId(),
                        request.decision() == VenueDecision.ALLOWED
                                ? RecruitmentNoticeRequestActionType.VENUE_ALLOW
                                : RecruitmentNoticeRequestActionType.VENUE_CANCEL,
                        previousDecision.name(),
                        request.decision().name(),
                        request.reason(),
                        adminId));
        return toResponseWithZones(entity);
    }

    private RecruitmentNoticeRequestResponse toResponseWithZones(RecruitmentNoticeRequest request) {
        List<Long> venueZoneIds =
                recruitmentNoticeRequestZoneRepository.findVenueZoneIdsByRequestId(request.getId());
        return recruitmentNoticeRequestConverter.toResponse(request, venueZoneIds);
    }
}
