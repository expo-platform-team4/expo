package com.expo.venue.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import com.expo.recruitment.entity.VenueDecision;
import com.expo.recruitment.repository.RecruitmentNoticeRequestRepository;
import com.expo.venue.converter.VenueReservationConverter;
import com.expo.venue.dto.CreateVenueReservationRequest;
import com.expo.venue.dto.VenueAvailabilityResponse;
import com.expo.venue.dto.VenueReservationResponse;
import com.expo.venue.entity.VenueReservation;
import com.expo.venue.entity.VenueReservationStatus;
import com.expo.venue.entity.VenueZone;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VenueReservationRepository;
import com.expo.venue.repository.VenueZoneRepository;
import com.expo.venue.repository.VirtualVenueRepository;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class VenueReservationService {

    private final VenueReservationRepository venueReservationRepository;
    private final RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository;
    private final VirtualVenueRepository virtualVenueRepository;
    private final VenueHallRepository venueHallRepository;
    private final VenueZoneRepository venueZoneRepository;
    private final VenueReservationConverter venueReservationConverter;

    public VenueReservationService(
            VenueReservationRepository venueReservationRepository,
            RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository,
            VirtualVenueRepository virtualVenueRepository,
            VenueHallRepository venueHallRepository,
            VenueZoneRepository venueZoneRepository,
            VenueReservationConverter venueReservationConverter) {
        this.venueReservationRepository = venueReservationRepository;
        this.recruitmentNoticeRequestRepository = recruitmentNoticeRequestRepository;
        this.virtualVenueRepository = virtualVenueRepository;
        this.venueHallRepository = venueHallRepository;
        this.venueZoneRepository = venueZoneRepository;
        this.venueReservationConverter = venueReservationConverter;
    }

    /**
     * 모집공고 생성 요청 경로의 확정 장소 예약 생성.
     *
     * <p>같은 장소·기간 중복은 DB의 EXCLUDE 제약({@code ex_venue_reservations_period})이 최종적으로 막는다. 여기서는 빠른
     * 실패를 위해 기간 유효성만 먼저 검증한다.
     */
    @Transactional
    public VenueReservationResponse create(
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
        Long effectiveHallId =
                validateHierarchy(
                        request.virtualVenueId(), request.venueHallId(), request.venueZoneId());
        VenueReservation reservation =
                VenueReservation.confirmForRecruitmentNotice(
                        request.noticeRequestId(),
                        request.virtualVenueId(),
                        effectiveHallId,
                        request.venueZoneId(),
                        request.useStartAt(),
                        request.useEndAt(),
                        confirmedByAdminId);
        try {
            VenueReservation saved = venueReservationRepository.saveAndFlush(reservation);
            return venueReservationConverter.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("ex_venue_reservations_period")) {
                throw new BusinessException(ErrorCode.VENUE_RESERVATION_PERIOD_CONFLICT);
            }
            log.warn(
                    "장소 예약 저장 중 예상하지 못한 무결성 제약 위반. noticeRequestId={}",
                    request.noticeRequestId(),
                    e);
            throw e;
        }
    }

    /** 박람회 취소 시 확정 장소 예약 해제. */
    @Transactional
    public VenueReservationResponse release(Long reservationId) {
        VenueReservation reservation =
                venueReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.VENUE_RESERVATION_NOT_FOUND));
        if (reservation.getStatus() != VenueReservationStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.VENUE_RESERVATION_ALREADY_RELEASED);
        }
        reservation.release();
        return venueReservationConverter.toResponse(reservation);
    }

    /** 장소·홀·구역·기간 예약 가능 여부 조회. */
    @Transactional(readOnly = true)
    public VenueAvailabilityResponse checkAvailability(
            Long virtualVenueId,
            Long venueHallId,
            Long venueZoneId,
            LocalDateTime useStartAt,
            LocalDateTime useEndAt) {
        if (!useEndAt.isAfter(useStartAt)) {
            throw new BusinessException(ErrorCode.VENUE_RESERVATION_PERIOD_INVALID);
        }
        Long effectiveHallId = validateHierarchy(virtualVenueId, venueHallId, venueZoneId);
        boolean overlapping =
                venueReservationRepository.existsOverlapping(
                        virtualVenueId, effectiveHallId, venueZoneId, useStartAt, useEndAt);
        return new VenueAvailabilityResponse(!overlapping);
    }

    /**
     * 홀·구역이 지정한 장소·홀 소속인지 검증하고, 실제로 사용할 홀 ID를 반환한다.
     *
     * <p>구역만 지정하고 홀을 안 넘긴 경우, 구역이 속한 홀을 조회해서 검증 기준이자 반환값으로 삼는다. 저장되는 예약에도 이
     * 반환값을 써야 한다 — {@code venue_zone_id} 를 지정하면 {@code venue_hall_id} 도 반드시 있어야 한다는 DB 제약을
     * 만족시키기 위함이다.
     */
    private Long validateHierarchy(Long virtualVenueId, Long venueHallId, Long venueZoneId) {
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
