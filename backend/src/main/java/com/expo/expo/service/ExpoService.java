package com.expo.expo.service;

import com.expo.expo.domain.*;
import com.expo.expo.dto.ExpoRequests.*;
import com.expo.expo.dto.ExpoResponses.*;
import com.expo.expo.dto.ExpoSearchCondition;
import com.expo.expo.exception.ExpoStateException;
import com.expo.expo.repository.ExpoFileRepository;
import com.expo.expo.repository.ExpoRepository;
import com.expo.expo.repository.TicketTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpoService {

    private final ExpoRepository expoRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final ExpoFileRepository expoFileRepository;

    /* ==================== 클라이언트 기능 ==================== */

    /** 희-EXPO-01 박람회 임시저장 / 희-EXPO-17 개최 신청 시작 */
    @Transactional
    public Long saveDraft(ExpoDraftRequest request) {
        Expo expo = Expo.builder()
            .clientId(request.clientId())
            .categoryId(request.categoryId())
            .desiredVenue(request.desiredVenue())
            .title(request.title())
            .description(request.description())
            .startDate(request.startDate())
            .endDate(request.endDate())
            .thumbnailUrl(request.thumbnailUrl())
            .region(request.region())
            .build();
        return expoRepository.save(expo).getExpoId();
    }

    /** 희-EXPO-05 승인 전 클라이언트 직접 수정 (승인 후에는 도메인에서 차단 — 희-EXPO-06) */
    @Transactional
    public void update(Long expoId, ExpoUpdateRequest request) {
        Expo expo = getExpo(expoId);
        validateOwner(expo, request.clientId());
        expo.updateByClient(
            request.categoryId(), request.desiredVenue(), request.title(),
            request.description(), request.startDate(), request.endDate(),
            request.thumbnailUrl(), request.region()
        );
    }

    /** 희-EXPO-02 박람회 등록 및 심사 요청 */
    @Transactional
    public void submit(Long expoId, Long clientId) {
        Expo expo = getExpo(expoId);
        validateOwner(expo, clientId);
        expo.submit();
    }

    /** 취소 요청 */
    @Transactional
    public void requestCancellation(Long expoId, Long clientId, ExpoCancelRequest request) {
        Expo expo = getExpo(expoId);
        validateOwner(expo, clientId);
        expo.requestCancellation(request.cancelReason());
    }

    /** 희-EXPO-13/14/15 외부 링크·PDF·이미지·영상·카탈로그·리플렛 등록 */
    @Transactional
    public Long addFile(Long expoId, ExpoFileRequest request) {
        Expo expo = getExpo(expoId);
        // 희-EXPO-15: 카탈로그/리플렛의 대표 이미지 지정은 thumbnail_url로도 반영 가능
        ExpoFile file = ExpoFile.builder()
            .expoId(expo.getExpoId())
            .fileType(request.fileType())
            .url(request.url())
            .displayName(request.displayName())
            .build();
        return expoFileRepository.save(file).getExpoFileId();
    }

    /** 논리 삭제 */
    @Transactional
    public void delete(Long expoId, Long clientId) {
        Expo expo = getExpo(expoId);
        validateOwner(expo, clientId);
        expo.softDelete();
    }

    /* ==================== 관리자 기능 ==================== */

    /** 심사 시작 */
    @Transactional
    public void startReview(Long expoId) {
        getExpo(expoId).startReview();
    }

    /** 희-EXPO-09 승인 시 자동 공개 + 확정 장소 배정 → 희-SRCH-12 목록 자동 반영 */
    @Transactional
    public void approve(Long expoId, ExpoApproveRequest request) {
        getExpo(expoId).approve(request.venueId());
    }

    /** 반려 — 반려 사유는 심사 이력 도메인(타 담당)에 별도 기록 */
    @Transactional
    public void reject(Long expoId) {
        getExpo(expoId).reject();
    }

    /* ==================== 조회 ==================== */

    /**
     * 공개 박람회 목록 검색 (희-SRCH-01 ~ 13)
     * page는 1부터 시작하는 페이지 번호 방식 (희-SRCH-10)
     */
    public Page<ExpoCardResponse> search(ExpoSearchCondition condition, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        return expoRepository.searchPublished(
            emptyToNull(condition.keyword()),
            condition.categoryId(),
            emptyToNull(condition.region()),
            condition.fromDate(),
            condition.toDate(),
            condition.minPrice(),
            condition.maxPrice(),
            condition.saleStatus() == null ? null : condition.saleStatus().name(),
            condition.sortOrDefault().name(),
            pageable
        ).map(ExpoCardResponse::from);
    }

    /** 상세 조회 — 판매 상태(희-EXPO-10)를 조회 시점에 계산해 함께 반환 */
    public ExpoDetailResponse getDetail(Long expoId) {
        Expo expo = getExpo(expoId);
        List<TicketType> ticketTypes = ticketTypeRepository.findByExpoIdOrderByPriceAsc(expoId);
        List<ExpoFile> files = expoFileRepository.findByExpoIdOrderByCreatedAtAsc(expoId);

        SaleStatus saleStatus = SaleStatus.calculate(
            LocalDate.now(),
            expo.getStartDate(),
            expo.getEndDate(),
            !ticketTypes.isEmpty(),
            !ticketTypes.isEmpty() && ticketTypes.stream().allMatch(TicketType::isSoldOut)
        );
        return ExpoDetailResponse.of(expo, saleStatus, ticketTypes, files);
    }

    /** 클라이언트 본인 등록 목록 (상태 무관, 임시저장 포함) */
    public Page<ExpoDetailResponse> getMyExpos(Long clientId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        return expoRepository.findByClientIdAndIsDeletedFalse(clientId, pageable)
            .map(e -> getDetail(e.getExpoId()));
    }

    /* ==================== 내부 유틸 ==================== */

    private Expo getExpo(Long expoId) {
        return expoRepository.findByExpoIdAndIsDeletedFalse(expoId)
            .orElseThrow(() -> new EntityNotFoundException("박람회를 찾을 수 없습니다. id=" + expoId));
    }

    private void validateOwner(Expo expo, Long clientId) {
        if (!expo.isOwnedBy(clientId)) {
            throw new ExpoStateException("본인이 등록한 박람회만 처리할 수 있습니다.");
        }
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
