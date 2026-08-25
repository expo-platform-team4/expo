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
import com.expo.recruitment.entity.VenueConflictStatus;
import com.expo.recruitment.entity.VenueDecision;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRequestHistoryRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRequestRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRequestZoneRepository;
import com.expo.venue.entity.VenueReservation;
import com.expo.venue.entity.VenueReservationStatus;
import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VenueReservationRepository;
import com.expo.venue.repository.VenueZoneRepository;
import com.expo.venue.repository.VirtualVenueRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// 생성자를 직접 쓰지 않는다. 의존이 9개라 Checkstyle 의 ParameterNumber(최대 8)에 걸린다 -
// 스케줄러들과 같은 방식으로 Lombok 이 만들게 둔다.
@RequiredArgsConstructor
public class RecruitmentNoticeRequestService {

    private final RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository;
    private final RecruitmentNoticeRequestZoneRepository recruitmentNoticeRequestZoneRepository;
    private final RecruitmentNoticeRequestHistoryRepository
            recruitmentNoticeRequestHistoryRepository;
    private final VirtualVenueRepository virtualVenueRepository;
    private final VenueHallRepository venueHallRepository;
    private final VenueZoneRepository venueZoneRepository;
    private final VenueReservationRepository venueReservationRepository;
    private final RecruitmentNoticeRepository recruitmentNoticeRepository;
    private final RecruitmentNoticeRequestConverter recruitmentNoticeRequestConverter;

    /**
     * 모집공고 생성 요청 작성 및 제출. 희망 전시관(홀)이 실제로 존재하는지, 고른 구역이 전부 그 전시관 소속인지, 기간 순서가
     * 올바른지 검증한다. 별도의 초안 수정 단계가 없어 작성과 동시에 제출된다.
     *
     * <p>신청 기간과 행사 기간은 쌍끼리(시작&lt;종료)만이 아니라 서로도 맞물려야 한다 - 신청 마감이 행사 시작보다
     * 늦으면 행사가 시작한 뒤에야 모집을 마감하는 꼴이 된다. 신청 시작이 행사 시작보다 늦는 경우는 이 검증과 신청
     * 시작&lt;신청 마감 검증을 합치면 자동으로 걸러진다.
     *
     * <p>고른 구역이 전시관 소속인지는 서비스에서도 먼저 확인하지만, DB의 복합 FK({@code
     * fk_notice_request_zones_zone_in_hall})가 최종 방어선이다.
     *
     * <p>고른 구역 중 하나라도 희망 기간에 이미 확정된 예약과 겹치면 장소 충돌 상태를 CONFLICT_PENDING 으로
     * 남긴다 - 관리자가 {@link #decideVenue} 로 검토할 때 참고하도록. 최종 방어는 여전히 예약 확정 시점의 DB
     * EXCLUDE 제약이 맡는다.
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
        if (request.applicationEndAt().isAfter(request.eventStartAt())) {
            throw new BusinessException(ErrorCode.APPLICATION_PERIOD_EXCEEDS_EVENT_PERIOD);
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
        boolean anyZoneOverlapsExistingReservation =
                venueZoneIds.stream()
                        .anyMatch(
                                zoneId ->
                                        venueReservationRepository.existsOverlapping(
                                                request.virtualVenueId(),
                                                request.venueHallId(),
                                                zoneId,
                                                request.eventStartAt(),
                                                request.eventEndAt()));
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
        entity.submit(
                anyZoneOverlapsExistingReservation
                        ? VenueConflictStatus.CONFLICT_PENDING
                        : VenueConflictStatus.CLEAR);
        RecruitmentNoticeRequest saved = recruitmentNoticeRequestRepository.save(entity);
        for (Long zoneId : venueZoneIds) {
            recruitmentNoticeRequestZoneRepository.save(
                    RecruitmentNoticeRequestZone.create(
                            saved.getId(), request.venueHallId(), zoneId));
        }
        // 방금 만든 요청이다. 장소 예약도 공고도 아직 있을 수 없다.
        return recruitmentNoticeRequestConverter.toResponse(saved, venueZoneIds, false, false);
    }

    /** 주최 클라이언트 본인이 작성한 모집공고 생성 요청 목록 조회. */
    @Transactional(readOnly = true)
    public List<RecruitmentNoticeRequestResponse> listMine(Long hostClientId) {
        return toResponsesWithZones(
                recruitmentNoticeRequestRepository.findAllByHostClientId(hostClientId));
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
        return toResponsesWithZones(recruitmentNoticeRequestRepository.findAll());
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

    /**
     * 요청 한 건을 응답으로 바꾼다.
     *
     * <h2>왜 예약·공고 존재 여부를 같이 싣나</h2>
     *
     * 화면이 <b>"이 요청으로 초안을 만들 수 있는가"</b> 를 판단할 수 있어야 하기 때문이다. 예전에는
     * {@code status == APPROVED} 만 보고 골랐는데, 그 상태는 <b>장소 판정의 부산물</b>이다
     * ({@code RecruitmentNoticeRequest#decideVenue} 가 ALLOWED 면 APPROVED 로 바꾼다). 그래서
     * 장소만 허용하고 예약은 아직 안 잡은 요청도 목록에 떠 버렸고, 고르면 제출 단계에서야
     * "장소 예약이 없다" 로 막혔다.
     *
     * <p>조건은 {@code RecruitmentNoticeService#create} 가 검사하는 것과 같아야 한다. 어긋나면
     * 화면은 만들 수 있다고 하고 서버는 거절하는 상태로 돌아간다.
     */
    private RecruitmentNoticeRequestResponse toResponseWithZones(RecruitmentNoticeRequest request) {
        List<Long> venueZoneIds =
                recruitmentNoticeRequestZoneRepository.findVenueZoneIdsByRequestId(request.getId());
        return recruitmentNoticeRequestConverter.toResponse(
                request,
                venueZoneIds,
                isVenueReservationConfirmed(
                        venueReservationRepository.findAllByNoticeRequestId(request.getId())),
                recruitmentNoticeRepository.existsByRequestId(request.getId()));
    }

    /**
     * 장소 예약이 <b>초안을 만들 수 있는 상태</b>인가.
     *
     * <p>한 건도 없으면 아직 예약 전이고, 해제된 예약이 섞여 있으면 그 구역은 더 이상 우리 것이
     * 아니다. 둘 다 초안을 만들면 안 되는 상태다 — 구역마다 예약이 하나씩 생기므로 <b>전부</b>
     * 확정이어야 한다.
     */
    private boolean isVenueReservationConfirmed(List<VenueReservation> reservations) {
        return !reservations.isEmpty()
                && reservations.stream()
                        .allMatch(r -> r.getStatus() == VenueReservationStatus.CONFIRMED);
    }

    private List<RecruitmentNoticeRequestResponse> toResponsesWithZones(
            List<RecruitmentNoticeRequest> requests) {
        List<Long> requestIds = requests.stream().map(RecruitmentNoticeRequest::getId).toList();
        Map<Long, List<Long>> zoneIdsByRequestId =
                recruitmentNoticeRequestZoneRepository.findAllByIdRequestIdIn(requestIds).stream()
                        .collect(
                                Collectors.groupingBy(
                                        RecruitmentNoticeRequestZone::getRequestId,
                                        Collectors.mapping(
                                                RecruitmentNoticeRequestZone::getVenueZoneId,
                                                Collectors.toList())));
        // 예약·공고 존재 여부도 한 번에 모은다. 요청마다 조회하면 목록 하나에 쿼리가 요청 수만큼 늘어난다.
        Map<Long, List<VenueReservation>> reservationsByRequestId =
                venueReservationRepository.findAllByNoticeRequestIdIn(requestIds).stream()
                        .collect(Collectors.groupingBy(VenueReservation::getNoticeRequestId));
        Set<Long> requestIdsWithNotice =
                Set.copyOf(recruitmentNoticeRepository.findRequestIdsByRequestIdIn(requestIds));

        return requests.stream()
                .map(
                        request ->
                                recruitmentNoticeRequestConverter.toResponse(
                                        request,
                                        zoneIdsByRequestId.getOrDefault(request.getId(), List.of()),
                                        isVenueReservationConfirmed(
                                                reservationsByRequestId.getOrDefault(
                                                        request.getId(), List.of())),
                                        requestIdsWithNotice.contains(request.getId())))
                .toList();
    }
}
