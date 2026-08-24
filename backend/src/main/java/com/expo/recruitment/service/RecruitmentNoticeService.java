package com.expo.recruitment.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.participation.entity.ParticipationApplicationStatus;
import com.expo.participation.repository.ParticipationApplicationRepository;
import com.expo.recruitment.converter.RecruitmentNoticeConverter;
import com.expo.recruitment.dto.CreateRecruitmentNoticeRequest;
import com.expo.recruitment.dto.RecruitmentNoticeResponse;
import com.expo.recruitment.dto.RecruitmentNoticeScheduleSweepResult;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruitmentNoticeService {

    private final RecruitmentNoticeRepository recruitmentNoticeRepository;
    private final RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository;
    private final RecruitmentNoticeHistoryRepository recruitmentNoticeHistoryRepository;
    private final ParticipationApplicationRepository participationApplicationRepository;
    private final VenueReservationRepository venueReservationRepository;
    private final VenueReservationHistoryRepository venueReservationHistoryRepository;
    private final RecruitmentNoticeConverter recruitmentNoticeConverter;

    public RecruitmentNoticeService(
            RecruitmentNoticeRepository recruitmentNoticeRepository,
            RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository,
            RecruitmentNoticeHistoryRepository recruitmentNoticeHistoryRepository,
            ParticipationApplicationRepository participationApplicationRepository,
            VenueReservationRepository venueReservationRepository,
            VenueReservationHistoryRepository venueReservationHistoryRepository,
            RecruitmentNoticeConverter recruitmentNoticeConverter) {
        this.recruitmentNoticeRepository = recruitmentNoticeRepository;
        this.recruitmentNoticeRequestRepository = recruitmentNoticeRequestRepository;
        this.recruitmentNoticeHistoryRepository = recruitmentNoticeHistoryRepository;
        this.participationApplicationRepository = participationApplicationRepository;
        this.venueReservationRepository = venueReservationRepository;
        this.venueReservationHistoryRepository = venueReservationHistoryRepository;
        this.recruitmentNoticeConverter = recruitmentNoticeConverter;
    }

    /**
     * 기업 모집 공고 초안 생성. 근거 요청의 주최 클라이언트를 그대로 연결한다.
     *
     * <p>이 요청이 승인된 뒤 확정한 장소 예약(구역마다 한 건씩)을 전부 찾아 이 공고에 연결한다. 예약은 홀 확정
     * 단계({@code VenueReservationService.create()})에서 이미 만들어져 있어야 한다.
     *
     * <p>신청 종료일이 실제 장소 사용 시작일보다 늦으면 안 된다 - 행사장 사용이 이미 시작됐는데도 신규 신청을
     * 계속 받는 꼴이 된다. 예약이 여러 건(구역별)이면 가장 이른 사용 시작일을 기준으로 삼는다.
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
        Instant earliestVenueUseStartAt =
                reservations.stream()
                        .map(VenueReservation::getUseStartAt)
                        .min(Instant::compareTo)
                        .orElseThrow();
        if (request.applicationEndAt().isAfter(earliestVenueUseStartAt)) {
            throw new BusinessException(ErrorCode.APPLICATION_PERIOD_EXCEEDS_VENUE_PERIOD);
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

    /** 관리자용 기업 모집 공고 목록 조회. N+1 을 피하려고 목록에 담긴 공고들의 장소 예약을 한 번에 모아 조회한다. */
    @Transactional(readOnly = true)
    public List<RecruitmentNoticeResponse> list() {
        return toResponsesWithVenue(recruitmentNoticeRepository.findAll());
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
        String beforeData = noticeSnapshot(notice);
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
                        beforeData,
                        noticeSnapshot(notice),
                        null,
                        adminId));
        return toResponseWithVenue(notice);
    }

    /**
     * 공고 게시. 초안 상태에서만 가능하다.
     *
     * <p>신청 시작일이 아직 안 됐으면 SCHEDULED 로만 전환되고, 실제로 신청이 열리는 OPEN 전환은 {@link
     * #processSchedule()} 이 시작일 도래를 보고 별도로 처리한다.
     *
     * <p>{@code create()} 때 확정돼 있던 장소 예약이 초안으로 대기하는 사이 해제됐을 수 있어, 게시 직전에 다시
     * 확인한다.
     */
    @Transactional
    public RecruitmentNoticeResponse publish(Long noticeId, Long adminId) {
        RecruitmentNotice notice = getEntity(noticeId);
        RecruitmentNoticeStatus previousStatus = notice.getStatus();
        if (previousStatus != RecruitmentNoticeStatus.DRAFT) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_PUBLISHABLE);
        }
        List<VenueReservation> reservations =
                venueReservationRepository.findAllByRecruitmentNoticeId(noticeId);
        boolean anyNotConfirmed =
                reservations.stream()
                        .anyMatch(r -> r.getStatus() != VenueReservationStatus.CONFIRMED);
        if (anyNotConfirmed) {
            throw new BusinessException(ErrorCode.VENUE_RESERVATION_ALREADY_RELEASED);
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
     * 예정된 게시 시작·신청 마감을 일괄 처리한다. SCHEDULED 인 공고 중 신청 시작일이 지난 것은 OPEN 으로,
     * OPEN 인 공고 중 신청 종료일이 지난 것은 CLOSED 로 자동 전환한다.
     *
     * <p>사람이 직접 처리한 게 아니라 시스템 판단이라 처리자 없이 남길 수 없는 이력({@code
     * processed_by_admin_id} NOT NULL)은 공고를 만든 관리자 ID로 남긴다.
     *
     * <p>여러 번 호출해도 안전하다 - 이미 전환된 공고는 대상 조회 조건(SCHEDULED/OPEN)에서 다시 걸리지 않는다.
     * 다중 인스턴스에서의 중복 실행 방지 장치가 없어 아직 스케줄러는 붙이지 않았다({@code
     * InternalSettlementController} 와 동일한 판단).
     */
    @Transactional
    public RecruitmentNoticeScheduleSweepResult processSchedule() {
        Instant now = Instant.now();
        List<Long> activatedIds =
                recruitmentNoticeRepository
                        .findAllByStatusAndApplicationStartAtBefore(
                                RecruitmentNoticeStatus.SCHEDULED, now)
                        .stream()
                        .map(
                                notice -> {
                                    notice.activate();
                                    recruitmentNoticeHistoryRepository.save(
                                            RecruitmentNoticeHistory.create(
                                                    notice.getId(),
                                                    RecruitmentNoticeActionType.ACTIVATE,
                                                    "{\"status\": \"SCHEDULED\"}",
                                                    "{\"status\": \"OPEN\"}",
                                                    "예정된 신청 시작일 도래로 자동 게시",
                                                    notice.getCreatedByAdminId()));
                                    return notice.getId();
                                })
                        .toList();
        List<Long> expiredIds =
                recruitmentNoticeRepository
                        .findAllByStatusAndApplicationEndAtBefore(RecruitmentNoticeStatus.OPEN, now)
                        .stream()
                        .map(
                                notice -> {
                                    notice.expire();
                                    recruitmentNoticeHistoryRepository.save(
                                            RecruitmentNoticeHistory.create(
                                                    notice.getId(),
                                                    RecruitmentNoticeActionType.CLOSE,
                                                    "{\"status\": \"OPEN\"}",
                                                    "{\"status\": \"CLOSED\"}",
                                                    "신청 종료일 경과로 자동 마감",
                                                    notice.getCreatedByAdminId()));
                                    return notice.getId();
                                })
                        .toList();
        return new RecruitmentNoticeScheduleSweepResult(activatedIds, expiredIds);
    }

    /**
     * 모집공고 직권 취소. 결제·신청이 아직 없는 마감 전 공고(초안·예약·게시 중)만 취소할 수 있다.
     *
     * <p>결제 완료(SUBMITTED) 신청서가 하나라도 있으면 취소를 거부한다 - 이미 돈을 낸 기업이 있는 채로 공고가
     * 사라지면 배정·환불 처리가 안 된 채로 방치된다.
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
        if (participationApplicationRepository.existsByRecruitmentNoticeIdAndStatus(
                noticeId, ParticipationApplicationStatus.SUBMITTED)) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_HAS_SUBMITTED_APPLICATIONS);
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

    /** {@code update()} 감사 이력에 남길 스냅샷 - 상태만으론 실제로 뭐가 바뀌었는지 알 수 없어 수정 대상 필드를 담는다. */
    private String noticeSnapshot(RecruitmentNotice notice) {
        return "{\"title\": \""
                + notice.getTitle()
                + "\", \"applicationStartAt\": \""
                + notice.getApplicationStartAt()
                + "\", \"applicationEndAt\": \""
                + notice.getApplicationEndAt()
                + "\"}";
    }

    /** 게시 중인 기업 모집 공고 목록 조회 (공개). N+1 을 피하려고 목록에 담긴 공고들의 장소 예약을 한 번에 모아 조회한다. */
    @Transactional(readOnly = true)
    public List<RecruitmentNoticeResponse> listPublic() {
        return toResponsesWithVenue(
                recruitmentNoticeRepository.findAllByStatus(RecruitmentNoticeStatus.OPEN));
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

    private List<RecruitmentNoticeResponse> toResponsesWithVenue(List<RecruitmentNotice> notices) {
        List<Long> noticeIds = notices.stream().map(RecruitmentNotice::getId).toList();
        Map<Long, List<VenueReservation>> reservationsByNoticeId =
                venueReservationRepository.findAllByRecruitmentNoticeIdIn(noticeIds).stream()
                        .collect(Collectors.groupingBy(VenueReservation::getRecruitmentNoticeId));
        return notices.stream()
                .map(
                        notice ->
                                toResponseWithVenue(
                                        notice,
                                        reservationsByNoticeId.getOrDefault(
                                                notice.getId(), List.of())))
                .toList();
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
