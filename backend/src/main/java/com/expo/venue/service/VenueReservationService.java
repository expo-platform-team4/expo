package com.expo.venue.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.recruitment.entity.RecruitmentNotice;
import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import com.expo.recruitment.entity.RecruitmentNoticeStatus;
import com.expo.recruitment.entity.VenueDecision;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRequestRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRequestZoneRepository;
import com.expo.venue.converter.VenueReservationConverter;
import com.expo.venue.dto.CreateVenueReservationRequest;
import com.expo.venue.dto.VenueAvailabilityResponse;
import com.expo.venue.dto.VenueReservationHistoryResponse;
import com.expo.venue.dto.VenueReservationResponse;
import com.expo.venue.entity.VenueReservation;
import com.expo.venue.entity.VenueReservationActionType;
import com.expo.venue.entity.VenueReservationHistory;
import com.expo.venue.entity.VenueReservationStatus;
import com.expo.venue.repository.VenueReservationHistoryRepository;
import com.expo.venue.repository.VenueReservationRepository;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class VenueReservationService {

    private final VenueReservationRepository venueReservationRepository;
    private final VenueReservationHistoryRepository venueReservationHistoryRepository;
    private final RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository;
    private final RecruitmentNoticeRequestZoneRepository recruitmentNoticeRequestZoneRepository;
    private final RecruitmentNoticeRepository recruitmentNoticeRepository;
    private final VenueHierarchyValidator venueHierarchyValidator;
    private final VenueReservationConverter venueReservationConverter;

    public VenueReservationService(
            VenueReservationRepository venueReservationRepository,
            VenueReservationHistoryRepository venueReservationHistoryRepository,
            RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository,
            RecruitmentNoticeRequestZoneRepository recruitmentNoticeRequestZoneRepository,
            RecruitmentNoticeRepository recruitmentNoticeRepository,
            VenueHierarchyValidator venueHierarchyValidator,
            VenueReservationConverter venueReservationConverter) {
        this.venueReservationRepository = venueReservationRepository;
        this.venueReservationHistoryRepository = venueReservationHistoryRepository;
        this.recruitmentNoticeRequestRepository = recruitmentNoticeRequestRepository;
        this.recruitmentNoticeRequestZoneRepository = recruitmentNoticeRequestZoneRepository;
        this.recruitmentNoticeRepository = recruitmentNoticeRepository;
        this.venueHierarchyValidator = venueHierarchyValidator;
        this.venueReservationConverter = venueReservationConverter;
    }

    /**
     * 모집공고 생성 요청 경로의 확정 장소 예약 생성. 그 요청이 고른 구역 개수만큼 예약을 한 번에 확정한다 - 하나라도 겹쳐서
     * 실패하면 전체가 롤백된다(전부 성공 아니면 전부 실패).
     *
     * <p>같은 (장소, 홀, 구역) 계층의 기간 중복은 DB의 EXCLUDE 제약({@code ex_venue_reservations_period})이,
     * 홀 전체 예약과 그 아래 구역 단위 예약이 겹치는 계층 간 충돌은 트리거({@code
     * trg_venue_reservation_hierarchy_conflict})가 최종적으로 막는다. 여기서는 빠른 실패를 위해 기간 유효성만 먼저
     * 검증한다.
     */
    @Transactional
    public List<VenueReservationResponse> create(
            Long confirmedByAdminId, CreateVenueReservationRequest request) {
        if (!request.useEndAt().isAfter(request.useStartAt())) {
            throw new BusinessException(ErrorCode.VENUE_RESERVATION_PERIOD_INVALID);
        }
        RecruitmentNoticeRequest noticeRequest =
                recruitmentNoticeRequestRepository
                        .findById(request.noticeRequestId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.RECRUITMENT_NOTICE_REQUEST_NOT_FOUND));
        if (noticeRequest.getVenueDecision() != VenueDecision.ALLOWED) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_REQUEST_NOT_ALLOWED);
        }
        List<Long> venueZoneIds =
                recruitmentNoticeRequestZoneRepository.findVenueZoneIdsByRequestId(
                        request.noticeRequestId());
        if (venueZoneIds.isEmpty()) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_REQUEST_ZONES_EMPTY);
        }
        try {
            return venueZoneIds.stream()
                    .map(
                            zoneId -> {
                                VenueReservation reservation =
                                        VenueReservation.confirmForRecruitmentNotice(
                                                request.noticeRequestId(),
                                                noticeRequest.getVirtualVenueId(),
                                                noticeRequest.getVenueHallId(),
                                                zoneId,
                                                request.useStartAt(),
                                                request.useEndAt(),
                                                confirmedByAdminId);
                                VenueReservation saved =
                                        venueReservationRepository.saveAndFlush(reservation);
                                venueReservationHistoryRepository.save(
                                        VenueReservationHistory.create(
                                                saved.getId(),
                                                VenueReservationActionType.CONFIRMED,
                                                null,
                                                confirmedByAdminId));
                                return venueReservationConverter.toResponse(saved);
                            })
                    .toList();
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null
                    && (cause.contains("ex_venue_reservations_period")
                            || cause.contains("venue_reservation_hierarchy_conflict"))) {
                throw new BusinessException(ErrorCode.VENUE_RESERVATION_PERIOD_CONFLICT);
            }
            log.warn(
                    "장소 예약 저장 중 예상하지 못한 무결성 제약 위반. noticeRequestId={}",
                    request.noticeRequestId(),
                    e);
            throw e;
        }
    }

    /** 장소 예약 목록 조회 (페이지 단위). */
    @Transactional(readOnly = true)
    public Page<VenueReservationResponse> list(Pageable pageable) {
        return venueReservationRepository
                .findAll(pageable)
                .map(venueReservationConverter::toResponse);
    }

    /** 장소 예약 상세 조회. */
    @Transactional(readOnly = true)
    public VenueReservationResponse get(Long reservationId) {
        VenueReservation reservation =
                venueReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.VENUE_RESERVATION_NOT_FOUND));
        return venueReservationConverter.toResponse(reservation);
    }

    /**
     * 관리자 직권 장소 예약 해제.
     *
     * <p>진행 중인(DRAFT·SCHEDULED·OPEN) 모집공고에 이미 연결된 예약은 여기서 직접 해제할 수 없다 - 그 공고가
     * 여전히 이 장소를 쓰는 걸로 알고 있는 채로 예약만 조용히 풀리면, 공고는 계속 살아있는데 장소는 없는 상태가 된다.
     * 그 공고를 먼저 취소하면({@code RecruitmentNoticeService.cancel()}) 딸린 예약이 전부 함께 해제된다.
     *
     * <p>권한이 걸린 변경이라 처리 관리자·사유를 감사 이력({@code venue_reservation_histories})에 남긴다.
     */
    @Transactional
    public VenueReservationResponse release(Long reservationId, Long adminId, String reason) {
        VenueReservation reservation =
                venueReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.VENUE_RESERVATION_NOT_FOUND));
        if (reservation.getStatus() != VenueReservationStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.VENUE_RESERVATION_ALREADY_RELEASED);
        }
        if (reservation.getRecruitmentNoticeId() != null) {
            RecruitmentNoticeStatus noticeStatus =
                    recruitmentNoticeRepository
                            .findById(reservation.getRecruitmentNoticeId())
                            .map(RecruitmentNotice::getStatus)
                            .orElse(null);
            if (noticeStatus == RecruitmentNoticeStatus.DRAFT
                    || noticeStatus == RecruitmentNoticeStatus.SCHEDULED
                    || noticeStatus == RecruitmentNoticeStatus.OPEN) {
                throw new BusinessException(ErrorCode.VENUE_RESERVATION_LINKED_TO_ACTIVE_NOTICE);
            }
        }
        reservation.release();
        venueReservationHistoryRepository.save(
                VenueReservationHistory.create(
                        reservation.getId(), VenueReservationActionType.RELEASED, reason, adminId));
        return venueReservationConverter.toResponse(reservation);
    }

    /** 장소 예약별 확정·해제 이력 조회. 최신순. */
    @Transactional(readOnly = true)
    public List<VenueReservationHistoryResponse> listHistory(Long reservationId) {
        if (!venueReservationRepository.existsById(reservationId)) {
            throw new BusinessException(ErrorCode.VENUE_RESERVATION_NOT_FOUND);
        }
        return venueReservationHistoryRepository
                .findAllByVenueReservationIdOrderByCreatedAtDescIdDesc(reservationId)
                .stream()
                .map(venueReservationConverter::toHistoryResponse)
                .toList();
    }

    /** 장소·홀·구역·기간 예약 가능 여부 조회. */
    @Transactional(readOnly = true)
    public VenueAvailabilityResponse checkAvailability(
            Long virtualVenueId,
            Long venueHallId,
            Long venueZoneId,
            Instant useStartAt,
            Instant useEndAt) {
        if (!useEndAt.isAfter(useStartAt)) {
            throw new BusinessException(ErrorCode.VENUE_RESERVATION_PERIOD_INVALID);
        }
        Long effectiveHallId =
                venueHierarchyValidator.validate(virtualVenueId, venueHallId, venueZoneId);
        boolean overlapping =
                venueReservationRepository.existsOverlapping(
                        virtualVenueId, effectiveHallId, venueZoneId, useStartAt, useEndAt);
        return new VenueAvailabilityResponse(!overlapping);
    }
}
