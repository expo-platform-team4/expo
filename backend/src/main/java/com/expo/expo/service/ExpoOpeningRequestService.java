package com.expo.expo.service;

import com.expo.auth.repository.ClientProfileRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.converter.ExpoOpeningRequestConverter;
import com.expo.expo.dto.CreateExpoOpeningRequestRequest;
import com.expo.expo.dto.ExpoOpeningRequestPayload;
import com.expo.expo.dto.ExpoOpeningRequestResponse;
import com.expo.expo.dto.UpdateExpoOpeningRequestRequest;
import com.expo.expo.entity.DesiredVenue;
import com.expo.expo.entity.Expo;
import com.expo.expo.entity.ExpoOpeningRequest;
import com.expo.expo.entity.ExpoOpeningRequestStatus;
import com.expo.expo.entity.ExpoPeriod;
import com.expo.expo.entity.ExpoReviewHistory;
import com.expo.expo.entity.ReviewDecision;
import com.expo.expo.repository.ExpoOpeningRequestRepository;
import com.expo.expo.repository.ExpoRepository;
import com.expo.expo.repository.ExpoReviewHistoryRepository;
import com.expo.venue.entity.VirtualVenue;
import com.expo.venue.repository.VirtualVenueRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 박람회 개최 신청 작성·심사 (이슈 #116).
 *
 * <p>모집공고 생성 요청({@link com.expo.recruitment.service.RecruitmentNoticeRequestService})과 같은 모양이다 —
 * 주최사가 올리고 관리자가 승인·반려한다. 다른 점은 <b>승인이 실제 {@code expos} 행을 만든다</b>는 것이다.
 */
@Service
@RequiredArgsConstructor
public class ExpoOpeningRequestService {

    private final ExpoOpeningRequestRepository openingRequestRepository;
    private final ExpoRepository expoRepository;
    private final ExpoReviewHistoryRepository reviewHistoryRepository;
    private final VirtualVenueRepository virtualVenueRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final ExpoOpeningRequestConverter converter;

    // ---------- 주최사 ----------

    @Transactional
    public ExpoOpeningRequestResponse create(
            Long hostClientId, CreateExpoOpeningRequestRequest request) {
        ExpoOpeningRequestPayload content = request.content();
        requireVenueExists(content.desiredVenueId());

        ExpoOpeningRequest saved =
                openingRequestRepository.save(
                        ExpoOpeningRequest.create(
                                hostClientId,
                                content.title(),
                                content.description(),
                                eventPeriodOf(content),
                                salesPeriodOf(content),
                                desiredVenueOf(content),
                                request.submitNow(),
                                Instant.now()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExpoOpeningRequestResponse> listMine(Long hostClientId) {
        return toResponses(openingRequestRepository.findByHostClientIdOrderByIdDesc(hostClientId));
    }

    @Transactional(readOnly = true)
    public ExpoOpeningRequestResponse getMine(Long requestId, Long hostClientId) {
        return toResponse(findOwned(requestId, hostClientId));
    }

    @Transactional
    public ExpoOpeningRequestResponse update(
            Long requestId, Long hostClientId, UpdateExpoOpeningRequestRequest request) {
        ExpoOpeningRequestPayload content = request.content();
        requireVenueExists(content.desiredVenueId());

        ExpoOpeningRequest entity = findOwned(requestId, hostClientId);
        entity.updateContent(
                content.title(),
                content.description(),
                eventPeriodOf(content),
                salesPeriodOf(content),
                desiredVenueOf(content));
        return toResponse(entity);
    }

    @Transactional
    public ExpoOpeningRequestResponse submit(Long requestId, Long hostClientId) {
        ExpoOpeningRequest entity = findOwned(requestId, hostClientId);
        entity.submit(Instant.now());
        return toResponse(entity);
    }

    @Transactional
    public ExpoOpeningRequestResponse cancel(Long requestId, Long hostClientId) {
        ExpoOpeningRequest entity = findOwned(requestId, hostClientId);
        entity.cancel();
        return toResponse(entity);
    }

    // ---------- 관리자 ----------

    @Transactional(readOnly = true)
    public List<ExpoOpeningRequestResponse> listForAdmin(ExpoOpeningRequestStatus status) {
        List<ExpoOpeningRequest> requests =
                status == null
                        ? openingRequestRepository.findAllByOrderByIdDesc()
                        : openingRequestRepository.findByStatusOrderByIdDesc(status);
        return toResponses(requests);
    }

    @Transactional(readOnly = true)
    public ExpoOpeningRequestResponse getForAdmin(Long requestId) {
        return toResponse(findOrThrow(requestId));
    }

    /**
     * 승인한다. 신청서를 APPROVED 로 바꾸고 <b>같은 트랜잭션에서</b> {@code expos} 행과 심사 이력을 만든다.
     *
     * <p>{@code region_code} 는 희망 장소에서 가져온다 — 신청서에 지역 컬럼이 없는데 {@code expos} 는 NOT NULL 이라
     * 값을 지어내지 않으려면 이 방법뿐이다.
     */
    @Transactional
    public ExpoOpeningRequestResponse approve(Long requestId, Long adminId) {
        ExpoOpeningRequest entity = findOrThrow(requestId);
        Instant now = Instant.now();

        String fromStatus = entity.getStatus().name();
        entity.approve(adminId, now);

        VirtualVenue venue = findVenueOrThrow(entity.getDesiredVenueId());
        Expo expo =
                expoRepository.save(
                        Expo.createFromOpeningRequest(entity, venue.getRegionCode(), adminId, now));

        reviewHistoryRepository.save(
                ExpoReviewHistory.record(
                        expo.getId(),
                        adminId,
                        ReviewDecision.APPROVE,
                        null,
                        fromStatus,
                        entity.getStatus().name(),
                        now));

        return toResponse(entity);
    }

    /**
     * 반려한다.
     *
     * <p>승인과 달리 심사 이력을 남기지 않는다 — {@code expo_review_histories.expo_id} 가 NOT NULL 인데 반려된
     * 신청은 박람회를 만들지 않아 가리킬 대상이 없다. 반려 사유·심사자·시각은 신청서 자체에 남는다.
     */
    @Transactional
    public ExpoOpeningRequestResponse reject(Long requestId, Long adminId, String reason) {
        ExpoOpeningRequest entity = findOrThrow(requestId);
        entity.reject(adminId, reason, Instant.now());
        return toResponse(entity);
    }

    // ---------- 내부 ----------

    private static ExpoPeriod eventPeriodOf(ExpoOpeningRequestPayload content) {
        return new ExpoPeriod(content.eventStartAt(), content.eventEndAt());
    }

    private static ExpoPeriod salesPeriodOf(ExpoOpeningRequestPayload content) {
        return new ExpoPeriod(content.salesStartAt(), content.salesEndAt());
    }

    private static DesiredVenue desiredVenueOf(ExpoOpeningRequestPayload content) {
        return new DesiredVenue(
                content.desiredVenueId(),
                content.desiredVenueHallId(),
                content.desiredVenueZoneId());
    }

    private ExpoOpeningRequest findOrThrow(Long requestId) {
        return openingRequestRepository
                .findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_NOT_FOUND));
    }

    /** 남의 신청은 "없는 것" 으로 다룬다 — 존재 여부를 흘리지 않는다. */
    private ExpoOpeningRequest findOwned(Long requestId, Long hostClientId) {
        ExpoOpeningRequest entity = findOrThrow(requestId);
        if (!entity.isOwnedBy(hostClientId)) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_NOT_FOUND);
        }
        return entity;
    }

    private void requireVenueExists(Long venueId) {
        if (venueId == null) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_VENUE_REQUIRED);
        }
        findVenueOrThrow(venueId);
    }

    private VirtualVenue findVenueOrThrow(Long venueId) {
        if (venueId == null) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_VENUE_REQUIRED);
        }
        return virtualVenueRepository
                .findById(venueId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIRTUAL_VENUE_NOT_FOUND));
    }

    private ExpoOpeningRequestResponse toResponse(ExpoOpeningRequest request) {
        return toResponses(List.of(request)).get(0);
    }

    /** 목록에서 회사명·장소명을 건건이 조회하지 않도록 한 번에 모아 읽는다(N+1 방지). */
    private List<ExpoOpeningRequestResponse> toResponses(List<ExpoOpeningRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }

        Map<Long, String> companyNames =
                clientProfileRepository
                        .findAllById(
                                requests.stream().map(ExpoOpeningRequest::getHostClientId).toList())
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        profile -> profile.getUserId(),
                                        profile -> profile.getCompanyName(),
                                        (first, second) -> first));

        Map<Long, String> venueNames =
                virtualVenueRepository
                        .findAllById(
                                requests.stream()
                                        .map(ExpoOpeningRequest::getDesiredVenueId)
                                        .filter(java.util.Objects::nonNull)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(VirtualVenue::getId, VirtualVenue::getName));

        Map<Long, Long> expoIdsByRequest =
                expoRepository
                        .findByOpeningRequestIdIn(
                                requests.stream().map(ExpoOpeningRequest::getId).toList())
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Expo::getOpeningRequestId,
                                        Expo::getId,
                                        (first, second) -> first));

        Function<ExpoOpeningRequest, ExpoOpeningRequestResponse> map =
                request ->
                        converter.toResponse(
                                request,
                                companyNames.get(request.getHostClientId()),
                                Optional.ofNullable(request.getDesiredVenueId())
                                        .map(venueNames::get)
                                        .orElse(null),
                                expoIdsByRequest.get(request.getId()));

        return requests.stream().map(map).toList();
    }
}
