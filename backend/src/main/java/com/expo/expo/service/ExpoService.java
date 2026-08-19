package com.expo.expo.service;

import com.expo.expo.dto.ExpoDto.*;
import com.expo.expo.entity.Expo;
import com.expo.expo.entity.ExpoAttachments.*;
import com.expo.expo.entity.ExpoEnums.SaleStatus;
import com.expo.expo.entity.ExpoOpeningRequest;
import com.expo.expo.exception.ExpoStateException;
import com.expo.expo.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpoService {

    private final ExpoRepository expoRepository;
    private final ExpoOpeningRequestRepository openingRequestRepository;
    private final ExpoImageRepository expoImageRepository;
    private final ExpoFileRepository expoFileRepository;
    private final ExpoExternalLinkRepository expoExternalLinkRepository;
    private final ExpoChangeRequestRepository changeRequestRepository;
    private final EntityManager em;

    /* ==================== 개최 신청 (클라이언트) ==================== */

    /** 희-EXPO-01 임시저장 / 희-EXPO-17 개최 신청 시작 — expo_opening_requests DRAFT 생성 */
    @Transactional
    public Long createOpeningRequest(OpeningRequestCreate req) {
        ExpoOpeningRequest request =
                ExpoOpeningRequest.builder()
                        .hostClientId(req.hostClientId())
                        .title(req.title())
                        .description(req.description())
                        .eventStartAt(req.eventStartAt())
                        .eventEndAt(req.eventEndAt())
                        .salesStartAt(req.salesStartAt())
                        .salesEndAt(req.salesEndAt())
                        .desiredVenueId(req.desiredVenueId())
                        .desiredVenueHallId(req.desiredVenueHallId())
                        .desiredVenueZoneId(req.desiredVenueZoneId())
                        .build();
        return openingRequestRepository.save(request).getId();
    }

    /** 희-EXPO-05 승인 전 직접 수정 (APPROVED 이후엔 도메인에서 차단 — 희-EXPO-06) */
    @Transactional
    public void updateOpeningRequest(Long requestId, OpeningRequestUpdate req) {
        ExpoOpeningRequest request = getOpeningRequest(requestId);
        validateOwner(request.isOwnedBy(req.hostClientId()));
        request.updateByClient(
                req.title(),
                req.description(),
                req.eventStartAt(),
                req.eventEndAt(),
                req.salesStartAt(),
                req.salesEndAt(),
                req.desiredVenueId(),
                req.desiredVenueHallId(),
                req.desiredVenueZoneId());
    }

    /** 희-EXPO-02 등록 및 심사 요청 */
    @Transactional
    public void submitOpeningRequest(Long requestId, Long clientId) {
        ExpoOpeningRequest request = getOpeningRequest(requestId);
        validateOwner(request.isOwnedBy(clientId));
        request.submit();
    }

    /** 신청 취소 (승인 전) */
    @Transactional
    public void cancelOpeningRequest(Long requestId, Long clientId) {
        ExpoOpeningRequest request = getOpeningRequest(requestId);
        validateOwner(request.isOwnedBy(clientId));
        request.cancel();
    }

    public Page<OpeningRequestResponse> getMyOpeningRequests(Long clientId, int page, int size) {
        return openingRequestRepository
                .findByHostClientId(clientId, pageOf(page, size))
                .map(OpeningRequestResponse::from);
    }

    public OpeningRequestResponse getOpeningRequestDetail(Long requestId) {
        return OpeningRequestResponse.from(getOpeningRequest(requestId));
    }

    /* ==================== 심사 (관리자) ==================== */

    @Transactional
    public void startReview(Long requestId, Long adminId) {
        getOpeningRequest(requestId).startReview(adminId);
    }

    /**
     * 희-EXPO-09 승인 → expos 생성 + PUBLIC 자동 공개 → 목록 자동 반영(희-SRCH-12)
     * region_code 는 요청값 > 희망 장소(virtual_venues.region_code) 순으로 해석.
     * 카테고리(expo_categories N:M)도 함께 연결한다.
     */
    @Transactional
    public Long approveOpeningRequest(Long requestId, OpeningRequestApprove req) {
        ExpoOpeningRequest request = getOpeningRequest(requestId);
        request.approve(req.adminId());

        String regionCode = resolveRegionCode(req.regionCode(), request.getDesiredVenueId());
        Expo expo = expoRepository.save(Expo.publishFrom(request, regionCode, req.adminId()));

        if (req.categoryIds() != null) {
            for (Long categoryId : new java.util.LinkedHashSet<>(req.categoryIds())) {
                em.createNativeQuery(
                                "INSERT INTO expo_categories (expo_id, category_id) VALUES (:expoId, :categoryId)"
                                        + " ON CONFLICT DO NOTHING")
                        .setParameter("expoId", expo.getId())
                        .setParameter("categoryId", categoryId)
                        .executeUpdate();
            }
        }
        return expo.getId();
    }

    @Transactional
    public void rejectOpeningRequest(Long requestId, OpeningRequestReject req) {
        getOpeningRequest(requestId).reject(req.adminId(), req.rejectionReason());
    }

    /* ==================== 첨부 자료 (희-EXPO-13~15) ==================== */

    /** 희-EXPO-15 대표 이미지(THUMBNAIL)·상세 이미지 등록 */
    @Transactional
    public Long addImage(Long expoId, ExpoImageCreate req) {
        requireExpo(expoId);
        return expoImageRepository
                .save(
                        ExpoImage.builder()
                                .expoId(expoId)
                                .fileId(req.fileId())
                                .imageType(req.imageType())
                                .altText(req.altText())
                                .sortOrder(req.sortOrder())
                                .build())
                .getId();
    }

    /** 희-EXPO-14/15 PDF·카탈로그·리플렛·홍보영상 등록 */
    @Transactional
    public Long addFile(Long expoId, ExpoFileCreate req) {
        requireExpo(expoId);
        return expoFileRepository
                .save(
                        ExpoFile.builder()
                                .expoId(expoId)
                                .fileId(req.fileId())
                                .filePurpose(req.filePurpose())
                                .title(req.title())
                                .sortOrder(req.sortOrder())
                                .build())
                .getId();
    }

    /** 희-EXPO-13 외부 링크 등록 */
    @Transactional
    public Long addExternalLink(Long expoId, ExternalLinkCreate req) {
        requireExpo(expoId);
        return expoExternalLinkRepository
                .save(
                        ExternalLink.builder()
                                .expoId(expoId)
                                .linkType(req.linkType())
                                .label(req.label())
                                .url(req.url())
                                .sortOrder(req.sortOrder())
                                .build())
                .getId();
    }

    /* ==================== 승인 후 수정 요청 (희-EXPO-06 대응 경로) ==================== */

    @Transactional
    public Long createChangeRequest(Long expoId, ChangeRequestCreate req) {
        Expo expo = requireExpo(expoId);
        validateOwner(expo.isOwnedBy(req.requesterClientId()));
        return changeRequestRepository
                .save(
                        ExpoChangeRequest.builder()
                                .expoId(expoId)
                                .requesterClientId(req.requesterClientId())
                                .changeReason(req.changeReason())
                                .requestedChanges(req.requestedChanges())
                                .build())
                .getId();
    }

    /* ==================== 공개 조회 (희-SRCH-01~13) ==================== */

    /** 목록 검색 — page 는 1부터 시작 (희-SRCH-10) */
    public Page<ExpoCardResponse> search(SearchCondition c, int page, int size) {
        return expoRepository
                .searchPublic(
                        emptyToNull(c.keyword()),
                        c.categoryId(),
                        emptyToNull(c.regionCode()),
                        c.fromDate(),
                        c.toDate(),
                        c.minPrice(),
                        c.maxPrice(),
                        c.saleStatus() == null ? null : c.saleStatus().name(),
                        c.sortOrDefault().name(),
                        pageOf(page, size))
                .map(ExpoCardResponse::from);
    }

    /** 상세 조회 — 판매 상태(희-EXPO-10) 계산 + 이미지·파일·링크 포함 */
    public ExpoDetailResponse getDetail(Long expoId) {
        Expo expo = requireExpo(expoId);

        // 티켓 상품 존재·전량 매진 여부 (ticket_products + ticket_inventories)
        Object[] agg =
                (Object[])
                        em.createNativeQuery(
                                        """
                        SELECT COUNT(*),
                               COALESCE(BOOL_AND(COALESCE(ti.available_quantity, 0) = 0), FALSE)
                        FROM ticket_products tp
                        LEFT JOIN ticket_inventories ti ON ti.ticket_product_id = tp.id
                        WHERE tp.expo_id = :expoId AND tp.status <> 'CANCELED'
                        """)
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

        return ExpoDetailResponse.of(
                expo,
                saleStatus,
                expoImageRepository.findByExpoIdOrderBySortOrderAscIdAsc(expoId),
                expoFileRepository.findByExpoIdOrderBySortOrderAscIdAsc(expoId),
                expoExternalLinkRepository.findByExpoIdOrderBySortOrderAscIdAsc(expoId));
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

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
