package com.expo.expo.service;

import com.expo.expo.dto.ExpoDto.ChangeRequestCreate;
import com.expo.expo.dto.ExpoDto.ExpoCardResponse;
import com.expo.expo.dto.ExpoDto.ExpoDetailResponse;
import com.expo.expo.dto.ExpoDto.ExpoFileCreate;
import com.expo.expo.dto.ExpoDto.ExpoImageCreate;
import com.expo.expo.dto.ExpoDto.ExternalLinkCreate;
import com.expo.expo.dto.ExpoDto.OpeningRequestApprove;
import com.expo.expo.dto.ExpoDto.OpeningRequestCreate;
import com.expo.expo.dto.ExpoDto.OpeningRequestReject;
import com.expo.expo.dto.ExpoDto.OpeningRequestResponse;
import com.expo.expo.dto.ExpoDto.OpeningRequestUpdate;
import com.expo.expo.dto.ExpoDto.SearchCondition;
import com.expo.expo.entity.Expo;
import com.expo.expo.entity.ExpoAttachments.ExpoChangeRequest;
import com.expo.expo.entity.ExpoAttachments.ExpoFile;
import com.expo.expo.entity.ExpoAttachments.ExpoImage;
import com.expo.expo.entity.ExpoAttachments.ExternalLink;
import com.expo.expo.entity.ExpoEnums.OpeningRequestStatus;
import com.expo.expo.entity.ExpoEnums.SaleStatus;
import com.expo.expo.entity.ExpoOpeningRequest;
import com.expo.expo.exception.ExpoStateException;
import com.expo.expo.repository.ExpoChangeRequestRepository;
import com.expo.expo.repository.ExpoExternalLinkRepository;
import com.expo.expo.repository.ExpoFileRepository;
import com.expo.expo.repository.ExpoImageRepository;
import com.expo.expo.repository.ExpoOpeningRequestRepository;
import com.expo.expo.repository.ExpoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 박람회 도메인 서비스.
 *
 * <p>클라이언트/관리자 메서드는 첫 인자로 인증 주체의 {@code memberId} 를 받는다. 컨트롤러가
 * {@code AuthPrincipal.getMemberId()} 로 넘겨주며, 본인 소유 여부는 여기서 재검증한다(위변조 방지).
 * 관리자 메서드의 {@code memberId} 는 처리자 기록용이다(권한 자체는 SecurityConfig 필터가 검증).
 */
@Service
@Transactional(readOnly = true)
public class ExpoService {

    private final ExpoRepository expoRepository;
    private final ExpoOpeningRequestRepository openingRequestRepository;
    private final ExpoImageRepository expoImageRepository;
    private final ExpoFileRepository expoFileRepository;
    private final ExpoExternalLinkRepository expoExternalLinkRepository;
    private final ExpoChangeRequestRepository changeRequestRepository;
    private final EntityManager em;

    public ExpoService(
            ExpoRepository expoRepository,
            ExpoOpeningRequestRepository openingRequestRepository,
            ExpoImageRepository expoImageRepository,
            ExpoFileRepository expoFileRepository,
            ExpoExternalLinkRepository expoExternalLinkRepository,
            ExpoChangeRequestRepository changeRequestRepository,
            EntityManager em) {
        this.expoRepository = expoRepository;
        this.openingRequestRepository = openingRequestRepository;
        this.expoImageRepository = expoImageRepository;
        this.expoFileRepository = expoFileRepository;
        this.expoExternalLinkRepository = expoExternalLinkRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.em = em;
    }

    /* ==================== 개최 신청 (클라이언트) ==================== */

    /** 희-EXPO-01 임시저장 / 희-EXPO-17 개최 신청 시작. 신청자는 인증 주체로 고정. */
    @Transactional
    public Long createOpeningRequest(Long memberId, OpeningRequestCreate request) {
        ExpoOpeningRequest entity =
                ExpoOpeningRequest.builder()
                        .hostClientId(memberId)
                        .title(request.title())
                        .description(request.description())
                        .eventStartAt(request.eventStartAt())
                        .eventEndAt(request.eventEndAt())
                        .salesStartAt(request.salesStartAt())
                        .salesEndAt(request.salesEndAt())
                        .desiredVenueId(request.desiredVenueId())
                        .desiredVenueHallId(request.desiredVenueHallId())
                        .desiredVenueZoneId(request.desiredVenueZoneId())
                        .build();
        return openingRequestRepository.save(entity).getId();
    }

    /** 희-EXPO-05 승인 전 직접 수정 (승인 후에는 도메인에서 차단 — 희-EXPO-06). */
    @Transactional
    public void updateOpeningRequest(Long memberId, Long requestId, OpeningRequestUpdate request) {
        ExpoOpeningRequest entity = getOpeningRequest(requestId);
        validateOwner(entity.isOwnedBy(memberId));
        entity.updateByClient(
                request.title(),
                request.description(),
                request.eventStartAt(),
                request.eventEndAt(),
                request.salesStartAt(),
                request.salesEndAt(),
                request.desiredVenueId(),
                request.desiredVenueHallId(),
                request.desiredVenueZoneId());
    }

    /** 희-EXPO-02 박람회 등록 및 심사 요청. */
    @Transactional
    public void submitOpeningRequest(Long memberId, Long requestId) {
        ExpoOpeningRequest entity = getOpeningRequest(requestId);
        validateOwner(entity.isOwnedBy(memberId));
        entity.submit();
    }

    /** 신청 취소 (승인 전). */
    @Transactional
    public void cancelOpeningRequest(Long memberId, Long requestId) {
        ExpoOpeningRequest entity = getOpeningRequest(requestId);
        validateOwner(entity.isOwnedBy(memberId));
        entity.cancel();
    }

    /** 내 개최 신청 목록 (임시저장 포함). */
    public Page<OpeningRequestResponse> getMyOpeningRequests(Long memberId, int page, int size) {
        return openingRequestRepository
                .findByHostClientId(memberId, pageOf(page, size))
                .map(OpeningRequestResponse::from);
    }

    /**
     * 내 개최 신청 상세 (API 명세 No.3). 본인 소유만 조회 가능하도록 검증하여 타인 신청 열람을 막는다.
     */
    public OpeningRequestResponse getMyOpeningRequestDetail(Long memberId, Long requestId) {
        ExpoOpeningRequest entity = getOpeningRequest(requestId);
        validateOwner(entity.isOwnedBy(memberId));
        return OpeningRequestResponse.from(entity);
    }

    /* ==================== 심사 (관리자) ==================== */

    /** 심사 시작. */
    @Transactional
    public void startReview(Long adminMemberId, Long requestId) {
        getOpeningRequest(requestId).startReview(adminMemberId);
    }

    /**
     * 희-EXPO-09 승인 → expos 생성 + PUBLIC 자동 공개 → 목록 자동 반영(희-SRCH-12). region_code 는
     * 요청값 > 희망 장소(virtual_venues.region_code) 순으로 해석하고, 카테고리(N:M)도 연결한다.
     */
    @Transactional
    public Long approveOpeningRequest(
            Long adminMemberId, Long requestId, OpeningRequestApprove request) {
        ExpoOpeningRequest entity = getOpeningRequest(requestId);
        entity.approve(adminMemberId);

        String regionCode = resolveRegionCode(request.regionCode(), entity.getDesiredVenueId());
        Expo expo = expoRepository.save(Expo.publishFrom(entity, regionCode, adminMemberId));

        if (request.categoryIds() != null) {
            for (Long categoryId : request.categoryIds()) {
                em.createNativeQuery(
                                "INSERT INTO expo_categories (expo_id, category_id)"
                                        + " VALUES (:expoId, :categoryId)")
                        .setParameter("expoId", expo.getId())
                        .setParameter("categoryId", categoryId)
                        .executeUpdate();
            }
        }
        return expo.getId();
    }

    /** 반려. */
    @Transactional
    public void rejectOpeningRequest(
            Long adminMemberId, Long requestId, OpeningRequestReject request) {
        getOpeningRequest(requestId).reject(adminMemberId, request.rejectionReason());
    }

    /** 관리자 개최 신청 목록·검색 (API 명세 No.6). status 가 null 이면 전체 조회. */
    public Page<OpeningRequestResponse> getOpeningRequests(
            OpeningRequestStatus status, int page, int size) {
        Pageable pageable = pageOf(page, size);
        Page<ExpoOpeningRequest> result =
                (status == null)
                        ? openingRequestRepository.findAll(pageable)
                        : openingRequestRepository.findByStatusOrderBySubmittedAtAsc(
                                status, pageable);
        return result.map(OpeningRequestResponse::from);
    }

    /** 관리자 개최 신청 상세 (API 명세 No.7). 관리자는 소유자 검증 없이 조회 가능. */
    public OpeningRequestResponse getOpeningRequestDetail(Long requestId) {
        return OpeningRequestResponse.from(getOpeningRequest(requestId));
    }

    /* ==================== 첨부 자료 (희-EXPO-13~15) — 후속 PR 대비 유지 ==================== */

    /** 희-EXPO-15 대표 이미지(THUMBNAIL)·상세 이미지 등록. */
    @Transactional
    public Long addImage(Long memberId, Long expoId, ExpoImageCreate request) {
        Expo expo = requireExpo(expoId);
        validateOwner(expo.isOwnedBy(memberId));
        return expoImageRepository
                .save(
                        ExpoImage.builder()
                                .expoId(expoId)
                                .fileId(request.fileId())
                                .imageType(request.imageType())
                                .altText(request.altText())
                                .sortOrder(request.sortOrder())
                                .build())
                .getId();
    }

    /** 희-EXPO-14/15 PDF·카탈로그·리플렛·홍보영상 등록. */
    @Transactional
    public Long addFile(Long memberId, Long expoId, ExpoFileCreate request) {
        Expo expo = requireExpo(expoId);
        validateOwner(expo.isOwnedBy(memberId));
        return expoFileRepository
                .save(
                        ExpoFile.builder()
                                .expoId(expoId)
                                .fileId(request.fileId())
                                .filePurpose(request.filePurpose())
                                .title(request.title())
                                .sortOrder(request.sortOrder())
                                .build())
                .getId();
    }

    /** 희-EXPO-13 외부 링크 등록. */
    @Transactional
    public Long addExternalLink(Long memberId, Long expoId, ExternalLinkCreate request) {
        Expo expo = requireExpo(expoId);
        validateOwner(expo.isOwnedBy(memberId));
        return expoExternalLinkRepository
                .save(
                        ExternalLink.builder()
                                .expoId(expoId)
                                .linkType(request.linkType())
                                .label(request.label())
                                .url(request.url())
                                .sortOrder(request.sortOrder())
                                .build())
                .getId();
    }

    /** 희-EXPO-06 대응 경로 — 승인 후 수정 요청. */
    @Transactional
    public Long createChangeRequest(Long memberId, Long expoId, ChangeRequestCreate request) {
        Expo expo = requireExpo(expoId);
        validateOwner(expo.isOwnedBy(memberId));
        return changeRequestRepository
                .save(
                        ExpoChangeRequest.builder()
                                .expoId(expoId)
                                .requesterClientId(memberId)
                                .changeReason(request.changeReason())
                                .requestedChanges(request.requestedChanges())
                                .build())
                .getId();
    }

    /* ==================== 공개 조회 (희-SRCH-01~13) ==================== */

    /** 목록 검색 — page 는 1부터 시작 (희-SRCH-10). */
    public Page<ExpoCardResponse> search(SearchCondition condition, int page, int size) {
        return expoRepository
                .searchPublic(
                        emptyToNull(condition.keyword()),
                        condition.categoryId(),
                        emptyToNull(condition.regionCode()),
                        condition.fromDate(),
                        condition.toDate(),
                        condition.minPrice(),
                        condition.maxPrice(),
                        condition.saleStatus() == null ? null : condition.saleStatus().name(),
                        condition.sortOrDefault().name(),
                        pageOf(page, size))
                .map(ExpoCardResponse::from);
    }

    /**
     * 상세 조회 — 판매 상태(희-EXPO-10) 계산 + 이미지·파일·링크 포함.
     *
     * <p>공개(PUBLIC·APPROVED·미취소) 상태가 아닌 박람회(DRAFT·심사중·반려·취소 등)는 비공개 리소스이므로
     * 노출하지 않는다. 존재 자체를 드러내지 않기 위해 404(찾을 수 없음)로 처리한다.
     */
    public ExpoDetailResponse getDetail(Long expoId) {
        Expo expo = requireExpo(expoId);
        if (!expo.isPubliclyVisible()) {
            throw new EntityNotFoundException("공개된 박람회를 찾을 수 없습니다. id=" + expoId);
        }

        Object[] agg =
                (Object[])
                        em.createNativeQuery(
                                        "SELECT COUNT(*),"
                                                + " COALESCE(BOOL_AND(COALESCE(ti.available_quantity, 0) = 0), FALSE)"
                                                + " FROM ticket_products tp"
                                                + " LEFT JOIN ticket_inventories ti"
                                                + " ON ti.ticket_product_id = tp.id"
                                                + " WHERE tp.expo_id = :expoId AND tp.status <> 'CANCELED'")
                                .setParameter("expoId", expoId)
                                .getSingleResult();
        boolean hasProducts = ((Number) agg[0]).longValue() > 0;
        boolean allSoldOut = (Boolean) agg[1];

        SaleStatus saleStatus =
                SaleStatus.calculate(
                        OffsetDateTime.now(),
                        expo.getEventEndAt(),
                        expo.getSalesStartAt(),
                        expo.getSalesEndAt(),
                        expo.getEventStatus(),
                        hasProducts,
                        allSoldOut);

        List<ExpoImage> images = expoImageRepository.findByExpoIdOrderBySortOrderAscIdAsc(expoId);
        List<ExpoFile> files = expoFileRepository.findByExpoIdOrderBySortOrderAscIdAsc(expoId);
        List<ExternalLink> links =
                expoExternalLinkRepository.findByExpoIdOrderBySortOrderAscIdAsc(expoId);
        return ExpoDetailResponse.of(expo, saleStatus, images, files, links);
    }

    /* ==================== 내부 유틸 ==================== */

    private ExpoOpeningRequest getOpeningRequest(Long id) {
        return openingRequestRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("개최 신청을 찾을 수 없습니다. id=" + id));
    }

    private Expo requireExpo(Long id) {
        return expoRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("박람회를 찾을 수 없습니다. id=" + id));
    }

    private String resolveRegionCode(String requested, Long desiredVenueId) {
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        if (desiredVenueId != null) {
            return expoRepository
                    .findVenueRegionCode(desiredVenueId)
                    .orElseThrow(() -> new ExpoStateException("희망 장소의 지역 코드를 찾을 수 없습니다."));
        }
        throw new ExpoStateException("지역 코드를 확인할 수 없습니다. regionCode 를 지정해 주세요.");
    }

    private void validateOwner(boolean owned) {
        if (!owned) {
            throw new ExpoStateException("본인이 등록한 건만 처리할 수 있습니다.");
        }
    }

    private Pageable pageOf(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), size);
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
