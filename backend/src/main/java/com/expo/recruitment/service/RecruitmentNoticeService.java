package com.expo.recruitment.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.recruitment.converter.RecruitmentNoticeConverter;
import com.expo.recruitment.dto.CreateRecruitmentNoticeRequest;
import com.expo.recruitment.dto.RecruitmentNoticeResponse;
import com.expo.recruitment.dto.UpdateRecruitmentNoticeRequest;
import com.expo.recruitment.entity.RecruitmentNotice;
import com.expo.recruitment.entity.RecruitmentNoticeActionType;
import com.expo.recruitment.entity.RecruitmentNoticeHistory;
import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import com.expo.recruitment.entity.RecruitmentNoticeStatus;
import com.expo.recruitment.entity.VenueDecision;
import com.expo.recruitment.repository.RecruitmentNoticeHistoryRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRequestRepository;
import com.expo.venue.entity.VenueReservation;
import com.expo.venue.entity.VenueReservationActionType;
import com.expo.venue.entity.VenueReservationHistory;
import com.expo.venue.entity.VenueReservationStatus;
import com.expo.venue.repository.VenueReservationHistoryRepository;
import com.expo.venue.repository.VenueReservationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruitmentNoticeService {

    private final RecruitmentNoticeRepository recruitmentNoticeRepository;
    private final RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository;
    private final RecruitmentNoticeHistoryRepository recruitmentNoticeHistoryRepository;
    private final VenueReservationRepository venueReservationRepository;
    private final VenueReservationHistoryRepository venueReservationHistoryRepository;
    private final RecruitmentNoticeConverter recruitmentNoticeConverter;

    public RecruitmentNoticeService(
            RecruitmentNoticeRepository recruitmentNoticeRepository,
            RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository,
            RecruitmentNoticeHistoryRepository recruitmentNoticeHistoryRepository,
            VenueReservationRepository venueReservationRepository,
            VenueReservationHistoryRepository venueReservationHistoryRepository,
            RecruitmentNoticeConverter recruitmentNoticeConverter) {
        this.recruitmentNoticeRepository = recruitmentNoticeRepository;
        this.recruitmentNoticeRequestRepository = recruitmentNoticeRequestRepository;
        this.recruitmentNoticeHistoryRepository = recruitmentNoticeHistoryRepository;
        this.venueReservationRepository = venueReservationRepository;
        this.venueReservationHistoryRepository = venueReservationHistoryRepository;
        this.recruitmentNoticeConverter = recruitmentNoticeConverter;
    }

    /**
     * 기업 모집 공고 초안 생성. 근거 요청의 주최 클라이언트를 그대로 연결한다.
     *
     * <p>이 요청이 승인된 뒤 확정한 장소 예약(구역마다 한 건씩)을 전부 찾아 이 공고에 연결한다. 예약은 홀 확정
     * 단계({@code VenueReservationService.create()})에서 이미 만들어져 있어야 한다.
     */
    @Transactional
    public RecruitmentNoticeResponse create(
            Long createdByAdminId, CreateRecruitmentNoticeRequest request) {
        if (!request.applicationEndAt().isAfter(request.applicationStartAt())) {
            throw new BusinessException(ErrorCode.APPLICATION_PERIOD_INVALID);
        }
        RecruitmentNoticeRequest noticeRequest =
                recruitmentNoticeRequestRepository
                        .findById(request.requestId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.RECRUITMENT_NOTICE_REQUEST_NOT_FOUND));
        if (noticeRequest.getVenueDecision() != VenueDecision.ALLOWED) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_CREATION_NOT_ALLOWED);
        }
        if (recruitmentNoticeRepository.existsByRequestId(request.requestId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RECRUITMENT_NOTICE_REQUEST);
        }
        List<VenueReservation> reservations =
                venueReservationRepository.findAllByNoticeRequestId(request.requestId());
        if (reservations.isEmpty()) {
            throw new BusinessException(ErrorCode.VENUE_RESERVATION_NOT_FOUND);
        }
        boolean anyNotConfirmed =
                reservations.stream()
                        .anyMatch(r -> r.getStatus() != VenueReservationStatus.CONFIRMED);
        if (anyNotConfirmed) {
            throw new BusinessException(ErrorCode.VENUE_RESERVATION_ALREADY_RELEASED);
        }
        RecruitmentNotice notice =
                RecruitmentNotice.create(
                                request.requestId(),
                                noticeRequest.getHostClientId(),
                                request.title(),
                                request.content(),
                                request.applicationStartAt(),
                                request.applicationEndAt(),
                                createdByAdminId)
                        .withDetails(request.eligibility(), request.submissionRequirements());
        RecruitmentNotice saved = recruitmentNoticeRepository.save(notice);
        reservations.forEach(r -> r.linkToNotice(saved.getId()));
        recruitmentNoticeHistoryRepository.save(
                RecruitmentNoticeHistory.create(
                        saved.getId(),
                        RecruitmentNoticeActionType.CREATE,
                        null,
                        "{\"status\": \"" + saved.getStatus() + "\"}",
                        null,
                        createdByAdminId));
        return toResponseWithVenue(saved, reservations);
    }

    /** 관리자용 기업 모집 공고 목록 조회. */
    @Transactional(readOnly = true)
    public List<RecruitmentNoticeResponse> list() {
        return recruitmentNoticeRepository.findAll().stream()
                .map(this::toResponseWithVenue)
                .toList();
    }

    /** 관리자용 기업 모집 공고 상세 조회. */
    @Transactional(readOnly = true)
    public RecruitmentNoticeResponse get(Long noticeId) {
        return toResponseWithVenue(getEntity(noticeId));
    }

    /** 공고 내용·조건 수정. 초안 상태에서만 가능하다. */
    @Transactional
    public RecruitmentNoticeResponse update(
            Long noticeId, Long adminId, UpdateRecruitmentNoticeRequest request) {
        if (!request.applicationEndAt().isAfter(request.applicationStartAt())) {
            throw new BusinessException(ErrorCode.APPLICATION_PERIOD_INVALID);
        }
        RecruitmentNotice notice = getEntity(noticeId);
        if (notice.getStatus() != RecruitmentNoticeStatus.DRAFT) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_EDITABLE);
        }
        notice.update(
                request.title(),
                request.content(),
                request.eligibility(),
                request.submissionRequirements(),
                request.applicationStartAt(),
                request.applicationEndAt());
        recruitmentNoticeHistoryRepository.save(
                RecruitmentNoticeHistory.create(
                        notice.getId(),
                        RecruitmentNoticeActionType.UPDATE,
                        "{\"status\": \"" + notice.getStatus() + "\"}",
                        "{\"status\": \"" + notice.getStatus() + "\"}",
                        null,
                        adminId));
        return toResponseWithVenue(notice);
    }

    /** 공고 게시. 초안 상태에서만 가능하다. */
    @Transactional
    public RecruitmentNoticeResponse publish(Long noticeId, Long adminId) {
        RecruitmentNotice notice = getEntity(noticeId);
        RecruitmentNoticeStatus previousStatus = notice.getStatus();
        if (previousStatus != RecruitmentNoticeStatus.DRAFT) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_PUBLISHABLE);
        }
        notice.publish();
        recruitmentNoticeHistoryRepository.save(
                RecruitmentNoticeHistory.create(
                        notice.getId(),
                        RecruitmentNoticeActionType.PUBLISH,
                        "{\"status\": \"" + previousStatus + "\"}",
                        "{\"status\": \"" + notice.getStatus() + "\"}",
                        null,
                        adminId));
        return toResponseWithVenue(notice);
    }

    /** 기업 모집 조기 마감. 게시 중인 공고만 마감할 수 있다. */
    @Transactional
    public RecruitmentNoticeResponse close(Long noticeId, Long adminId) {
        RecruitmentNotice notice = getEntity(noticeId);
        RecruitmentNoticeStatus previousStatus = notice.getStatus();
        if (previousStatus != RecruitmentNoticeStatus.OPEN) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_CLOSABLE);
        }
        notice.close();
        recruitmentNoticeHistoryRepository.save(
                RecruitmentNoticeHistory.create(
                        notice.getId(),
                        RecruitmentNoticeActionType.CLOSE,
                        "{\"status\": \"" + previousStatus + "\"}",
                        "{\"status\": \"" + notice.getStatus() + "\"}",
                        null,
                        adminId));
        return toResponseWithVenue(notice);
    }

    /**
     * 모집공고 직권 취소. 결제·신청이 아직 없는 마감 전 공고(초안·예약·게시 중)만 취소할 수 있다.
     *
     * <p>권한이 걸린 변경이라 변경 전후 상태를 감사 이력({@code recruitment_notice_histories})에 남긴다. 이 공고에
     * 딸린 장소 예약도 전부 같이 해제한다 - 안 풀면 같은 기간에 그 홀들을 다시 못 쓴다.
     */
    @Transactional
    public RecruitmentNoticeResponse cancel(Long noticeId, Long adminId, String reason) {
        RecruitmentNotice notice = getEntity(noticeId);
        RecruitmentNoticeStatus previousStatus = notice.getStatus();
        if (previousStatus != RecruitmentNoticeStatus.DRAFT
                && previousStatus != RecruitmentNoticeStatus.SCHEDULED
                && previousStatus != RecruitmentNoticeStatus.OPEN) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_CANCELABLE);
        }
        notice.cancel();
        recruitmentNoticeHistoryRepository.save(
                RecruitmentNoticeHistory.create(
                        notice.getId(),
                        RecruitmentNoticeActionType.CANCEL,
                        "{\"status\": \"" + previousStatus + "\"}",
                        "{\"status\": \"" + notice.getStatus() + "\"}",
                        reason,
                        adminId));
        String releaseReason = reason == null ? "모집공고 취소" : "모집공고 취소: " + reason;
        List<VenueReservation> reservations =
                venueReservationRepository.findAllByRecruitmentNoticeId(noticeId);
        reservations.stream()
                .filter(r -> r.getStatus() == VenueReservationStatus.CONFIRMED)
                .forEach(
                        r -> {
                            r.release();
                            venueReservationHistoryRepository.save(
                                    VenueReservationHistory.create(
                                            r.getId(),
                                            VenueReservationActionType.RELEASED,
                                            releaseReason,
                                            adminId));
                        });
        return toResponseWithVenue(notice, reservations);
    }

    private RecruitmentNotice getEntity(Long noticeId) {
        return recruitmentNoticeRepository
                .findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_FOUND));
    }

    /** 게시 중인 기업 모집 공고 목록 조회 (공개). */
    @Transactional(readOnly = true)
    public List<RecruitmentNoticeResponse> listPublic() {
        return recruitmentNoticeRepository.findAllByStatus(RecruitmentNoticeStatus.OPEN).stream()
                .map(this::toResponseWithVenue)
                .toList();
    }

    /** 기업 모집 공고 상세 조회 (공개). 게시 중인 공고만 조회할 수 있다. */
    @Transactional(readOnly = true)
    public RecruitmentNoticeResponse getPublic(Long noticeId) {
        RecruitmentNotice notice =
                recruitmentNoticeRepository
                        .findByIdAndStatus(noticeId, RecruitmentNoticeStatus.OPEN)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.RECRUITMENT_NOTICE_NOT_FOUND));
        return toResponseWithVenue(notice);
    }

    private RecruitmentNoticeResponse toResponseWithVenue(RecruitmentNotice notice) {
        return toResponseWithVenue(
                notice, venueReservationRepository.findAllByRecruitmentNoticeId(notice.getId()));
    }

    /** 확정된(취소된 것 제외) 예약들에서 전시관(홀)·구역 목록을 뽑아 응답에 채운다. */
    private RecruitmentNoticeResponse toResponseWithVenue(
            RecruitmentNotice notice, List<VenueReservation> reservations) {
        List<VenueReservation> confirmed =
                reservations.stream()
                        .filter(r -> r.getStatus() == VenueReservationStatus.CONFIRMED)
                        .toList();
        Long venueHallId =
                confirmed.stream().map(VenueReservation::getVenueHallId).findFirst().orElse(null);
        List<Long> venueZoneIds = confirmed.stream().map(VenueReservation::getVenueZoneId).toList();
        return recruitmentNoticeConverter.toResponse(notice, venueHallId, venueZoneIds);
    }
}
