package com.expo.banner.service;

import com.expo.banner.dto.ActiveBannerResponse;
import com.expo.banner.dto.BannerApplicationAdminResponse;
import com.expo.banner.dto.BannerApplicationCreateRequest;
import com.expo.banner.dto.BannerApplicationRejectRequest;
import com.expo.banner.dto.BannerApplicationResponse;
import com.expo.banner.entity.Banner;
import com.expo.banner.entity.Banner.DisplayStatus;
import com.expo.banner.entity.BannerApplication;
import com.expo.banner.entity.BannerApplication.ReviewStatus;
import com.expo.banner.entity.BannerReviewDecision;
import com.expo.banner.entity.BannerReviewHistory;
import com.expo.banner.entity.BannerSlot;
import com.expo.banner.repository.BannerApplicationRepository;
import com.expo.banner.repository.BannerRepository;
import com.expo.banner.repository.BannerReviewHistoryRepository;
import com.expo.banner.repository.BannerSlotRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 광고 배너 도메인 서비스.
 *
 * <p>관리자 메서드의 adminId 는 처리자 기록용이다(권한 자체는 SecurityConfig 필터가 검증,
 * ExpoService 와 동일한 컨벤션). 목록 조회(B-API-020)는 clientUserId 로 <b>필터링해서</b> 읽으므로
 * 남의 것이 섞일 수 없다.
 *
 * <p>반면 <b>ID 로 특정 건을 지목하는 요청은 소유권을 따로 확인해야 한다.</b> 취소가 그것이다 —
 * 경로에 남의 신청 ID 를 넣으면 그대로 처리되기 때문이다. {@link #cancelApplication} 이
 * {@code isOwnedBy} 로 막는다.
 *
 * <p>메인 배너 슬롯은 현재 단일 슬롯(MAIN_TOP, seed: V202608201545)만 존재하므로 승인·활성 조회 시
 * 이 슬롯을 기본으로 사용한다. 슬롯이 여러 개로 늘어나면 신청 시점에 slotCode 를 선택받도록 확장한다.
 */
@Service
@Transactional(readOnly = true)
public class BannerApplicationService {

    private static final String MAIN_SLOT_CODE = "MAIN_TOP";
    private static final List<DisplayStatus> QUEUED_STATUSES =
            List.of(DisplayStatus.SCHEDULED, DisplayStatus.ACTIVE);

    private final BannerApplicationRepository applicationRepository;
    private final BannerRepository bannerRepository;
    private final BannerSlotRepository bannerSlotRepository;
    private final BannerReviewHistoryRepository reviewHistoryRepository;

    public BannerApplicationService(
            BannerApplicationRepository applicationRepository,
            BannerRepository bannerRepository,
            BannerSlotRepository bannerSlotRepository,
            BannerReviewHistoryRepository reviewHistoryRepository) {
        this.applicationRepository = applicationRepository;
        this.bannerRepository = bannerRepository;
        this.bannerSlotRepository = bannerSlotRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
    }

    /* ==================== 배너 신청 (클라이언트) ==================== */

    /**
     * B-API-019: 배너 노출 신청. 별도 임시저장 단계 없이 등록과 동시에 심사 요청(UNDER_REVIEW)
     * 상태로 전환하고, 심사 이력에 SUBMIT 을 남긴다.
     */
    @Transactional
    public Long createApplication(Long clientUserId, BannerApplicationCreateRequest request) {
        BannerApplication application =
                BannerApplication.builder()
                        .clientUserId(clientUserId)
                        .expoId(request.expoId())
                        .imageFileId(request.imageFileId())
                        .headline(request.headline())
                        .requestedStartAt(request.requestedStartAt())
                        .requestedEndAt(request.requestedEndAt())
                        .build();
        application.submit();
        applicationRepository.save(application);

        recordHistory(
                application, BannerReviewDecision.SUBMIT, null, null, ReviewStatus.UNDER_REVIEW);
        return application.getId();
    }

    /** B-API-020: 내 배너 신청 목록·상태 조회. page 는 1부터 시작(ExpoService 컨벤션과 동일). */
    public Page<BannerApplicationResponse> getMyApplications(
            Long clientUserId, int page, int size) {
        return applicationRepository
                .findByClientUserIdOrderByIdDesc(clientUserId, pageOf(page, size))
                .map(BannerApplicationResponse::from);
    }

    /* ==================== 심사 (관리자) ==================== */

    /**
     * B-API-021: 관리자 배너 신청 목록·기간 충돌 조회. status 가 null 이면 전체 조회.
     * 심사 대기(UNDER_REVIEW) 건에 한해 현재 승인된 배너와 기간이 겹치는지 함께 계산한다.
     */
    public Page<BannerApplicationAdminResponse> getApplicationsForAdmin(
            ReviewStatus status, int page, int size) {
        BannerSlot slot = getMainSlot();
        return applicationRepository
                .searchForAdmin(status, pageOf(page, size))
                .map(
                        application ->
                                BannerApplicationAdminResponse.from(
                                        application, hasPeriodConflict(application, slot)));
    }

    /** B-API-021A: 관리자 배너 신청 상세 조회. 관리자는 소유자 검증 없이 조회 가능. */
    public BannerApplicationResponse getApplicationDetail(Long requestId) {
        return BannerApplicationResponse.from(getApplication(requestId));
    }

    /**
     * 목록의 {@code hasPeriodConflict} 를 드릴다운할 때 쓰는 상세 조회 — 실제로 겹치는
     * 배너 목록을 반환한다. (번호 없는 보조 엔드포인트: GET .../{requestId}/conflicts)
     */
    public List<ActiveBannerResponse> getConflictingBanners(Long requestId) {
        BannerApplication application = getApplication(requestId);
        BannerSlot slot = getMainSlot();
        return bannerRepository
                .findOverlapping(
                        slot.getId(),
                        QUEUED_STATUSES,
                        application.getRequestedStartAt(),
                        application.getRequestedEndAt())
                .stream()
                .map(ActiveBannerResponse::from)
                .toList();
    }

    /**
     * B-API-022: 배너 승인 및 즉시/예약 자동 활성화.
     *
     * <p>승인 자체는 정원(최대 노출 수)을 검사해 막지 않는다 — "최대 5개"는 조회 시점에
     * sortOrder 순으로 잘라내는 정책(B-API-024)이므로, 여기서는 Banner 를 만들어 큐 뒤쪽에
     * 이어붙이기만 한다.
     */
    @Transactional
    public Long approveApplication(Long adminId, Long requestId) {
        BannerApplication application = getApplication(requestId);
        ReviewStatus fromStatus = application.getReviewStatus();
        application.approve(adminId);

        BannerSlot slot = getMainSlot();
        OffsetDateTime now = OffsetDateTime.now();
        long queuedCount =
                bannerRepository.countByBannerSlotIdAndDisplayStatus(
                                slot.getId(), DisplayStatus.ACTIVE)
                        + bannerRepository.countByBannerSlotIdAndDisplayStatus(
                                slot.getId(), DisplayStatus.SCHEDULED);

        Banner banner = Banner.activateFrom(application, slot, (int) queuedCount, now);
        bannerRepository.save(banner);

        recordHistory(
                application, BannerReviewDecision.APPROVE, null, fromStatus, ReviewStatus.APPROVED);
        return banner.getId();
    }

    /** B-API-023: 배너 신청 반려 및 사유 저장. */
    @Transactional
    public void rejectApplication(
            Long adminId, Long requestId, BannerApplicationRejectRequest request) {
        BannerApplication application = getApplication(requestId);
        ReviewStatus fromStatus = application.getReviewStatus();
        application.reject(adminId, request.reason());

        recordHistory(
                application,
                BannerReviewDecision.REJECT,
                request.reason(),
                fromStatus,
                ReviewStatus.REJECTED);
    }

    /**
     * 주최사가 자기 신청을 취소한다.
     *
     * <h2>남의 신청을 취소할 수 없다</h2>
     *
     * 목록 조회와 달리 <b>경로에 ID 를 직접 받는다.</b> 로그인만 되어 있으면 남의 신청 번호를 넣어
     * 부를 수 있으므로 소유권을 확인한다. 목록이 clientUserId 로 걸러 읽는 것과는 다른 문제다.
     *
     * <h2>승인된 뒤에는 못 한다</h2>
     *
     * 승인은 이미 노출 배너를 만들어 슬롯 자리를 차지한 상태다. 신청서만 취소하면 <b>배너는 그대로
     * 노출되면서 신청서는 취소됨</b> 인 어긋난 상태가 된다. 엔티티가 이것을 막는다
     * ({@code BannerApplication#cancel}). 승인 후 내리는 것은 별도 기능이라 여기서 다루지 않는다.
     */
    @Transactional
    public void cancelApplication(Long clientUserId, Long requestId) {
        BannerApplication application = getApplication(requestId);
        if (!application.isOwnedBy(clientUserId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        ReviewStatus fromStatus = application.getReviewStatus();
        application.cancel();

        recordHistory(
                application, BannerReviewDecision.CANCEL, null, fromStatus, ReviewStatus.CANCELED);
    }

    /* ==================== 공개 조회 ==================== */

    /**
     * B-API-024: 현재 노출 가능한 메인 배너 조회 — ACTIVE 상태를 sortOrder(신청 순) 기준으로
     * 정렬하고, 슬롯의 max_active_count 만큼만 잘라 반환한다(하드코딩된 5가 아님).
     */
    public List<ActiveBannerResponse> getActiveBanners() {
        BannerSlot slot = getMainSlot();
        Pageable limit = PageRequest.of(0, slot.getMaxActiveCount());
        return bannerRepository.findActiveBySlot(slot.getId(), DisplayStatus.ACTIVE, limit).stream()
                .map(ActiveBannerResponse::from)
                .toList();
    }

    /* ==================== 내부 유틸 ==================== */

    /** 아직 심사 대기 중인 신청만 충돌을 계산한다 — 이미 처리된 건은 판정이 끝나 의미가 없다. */
    private boolean hasPeriodConflict(BannerApplication application, BannerSlot slot) {
        if (application.getReviewStatus() != ReviewStatus.UNDER_REVIEW) {
            return false;
        }
        return bannerRepository.existsOverlapping(
                slot.getId(),
                QUEUED_STATUSES,
                application.getRequestedStartAt(),
                application.getRequestedEndAt());
    }

    private void recordHistory(
            BannerApplication application,
            BannerReviewDecision decision,
            String reason,
            ReviewStatus fromStatus,
            ReviewStatus toStatus) {
        // 컬럼 이름은 reviewer_admin_id 지만 담기는 것은 "누가 했나" 다. 관리자가 아닌 주최사가
        // 움직이는 결정(제출·취소)에서는 주최사 ID 가 들어간다. NOT NULL 이라 비워 둘 수 없고,
        // 취소 시점의 reviewedByAdminId 는 아직 null 이다.
        boolean byClient =
                decision == BannerReviewDecision.SUBMIT || decision == BannerReviewDecision.CANCEL;
        Long reviewerId =
                byClient ? application.getClientUserId() : application.getReviewedByAdminId();
        reviewHistoryRepository.save(
                BannerReviewHistory.record(
                        application.getId(),
                        reviewerId,
                        decision,
                        reason,
                        fromStatus == null ? null : fromStatus.name(),
                        toStatus.name(),
                        OffsetDateTime.now().toInstant()));
    }

    private BannerApplication getApplication(Long id) {
        return applicationRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("배너 신청을 찾을 수 없습니다. id=" + id));
    }

    private BannerSlot getMainSlot() {
        return bannerSlotRepository
                .findBySlotCode(MAIN_SLOT_CODE)
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "배너 슬롯을 찾을 수 없습니다. slotCode=" + MAIN_SLOT_CODE));
    }

    /**
     * 목록 API 의 {@code page} 는 <b>1부터 센다.</b>
     *
     * <p>저장소 안에서도 통일되어 있지 않다 — 알림 이력 API 는 0부터 센다. 부르는 쪽이 규칙을
     * 반대로 알면 <b>조용히 틀린다:</b> 0을 보내면 {@code Math.max} 가 0으로 눌러 1페이지와 같은
     * 결과를 주고, 에러도 경고도 없다. 프론트는 {@code features/banner/api.ts} 에서 1부터 센다.
     */
    private Pageable pageOf(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), size);
    }
}
