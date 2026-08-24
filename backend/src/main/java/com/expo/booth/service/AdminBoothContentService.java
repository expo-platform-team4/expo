package com.expo.booth.service;

import com.expo.booth.converter.BoothContentConverter;
import com.expo.booth.converter.BoothManagementHistoryConverter;
import com.expo.booth.dto.BoothContentResponse;
import com.expo.booth.dto.BoothManagementHistoryResponse;
import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.entity.BoothAllocationStatus;
import com.expo.booth.entity.BoothContent;
import com.expo.booth.entity.BoothContentFile;
import com.expo.booth.entity.BoothContentStatus;
import com.expo.booth.entity.BoothManagementActionType;
import com.expo.booth.entity.BoothManagementHistory;
import com.expo.booth.entity.ExternalLink;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothContentFileRepository;
import com.expo.booth.repository.BoothContentRepository;
import com.expo.booth.repository.BoothManagementHistoryRepository;
import com.expo.booth.repository.ExternalLinkRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자용 부스 콘텐츠 조회와 운영 확인·보완 요청·숨김 처리. */
@Service
public class AdminBoothContentService {

    private final BoothContentRepository boothContentRepository;
    private final BoothContentFileRepository boothContentFileRepository;
    private final ExternalLinkRepository externalLinkRepository;
    private final BoothAllocationRepository boothAllocationRepository;
    private final BoothManagementHistoryRepository boothManagementHistoryRepository;
    private final BoothContentConverter boothContentConverter;
    private final BoothManagementHistoryConverter boothManagementHistoryConverter;

    public AdminBoothContentService(
            BoothContentRepository boothContentRepository,
            BoothContentFileRepository boothContentFileRepository,
            ExternalLinkRepository externalLinkRepository,
            BoothAllocationRepository boothAllocationRepository,
            BoothManagementHistoryRepository boothManagementHistoryRepository,
            BoothContentConverter boothContentConverter,
            BoothManagementHistoryConverter boothManagementHistoryConverter) {
        this.boothContentRepository = boothContentRepository;
        this.boothContentFileRepository = boothContentFileRepository;
        this.externalLinkRepository = externalLinkRepository;
        this.boothAllocationRepository = boothAllocationRepository;
        this.boothManagementHistoryRepository = boothManagementHistoryRepository;
        this.boothContentConverter = boothContentConverter;
        this.boothManagementHistoryConverter = boothManagementHistoryConverter;
    }

    /** 부스 콘텐츠 목록 조회 (페이지 단위). N+1을 피하려고 페이지에 담긴 콘텐츠의 파일·링크를 한 번에 모아 조회한다. */
    @Transactional(readOnly = true)
    public Page<BoothContentResponse> list(Pageable pageable) {
        Page<BoothContent> page = boothContentRepository.findAll(pageable);
        List<Long> contentIds = page.map(BoothContent::getId).toList();

        Map<Long, List<BoothContentFile>> filesByContentId =
                boothContentFileRepository
                        .findAllByBoothContentIdInOrderBySortOrderAscIdAsc(contentIds)
                        .stream()
                        .collect(Collectors.groupingBy(BoothContentFile::getBoothContentId));
        Map<Long, List<ExternalLink>> linksByContentId =
                externalLinkRepository
                        .findAllByBoothContentIdInOrderBySortOrderAscIdAsc(contentIds)
                        .stream()
                        .collect(Collectors.groupingBy(ExternalLink::getBoothContentId));

        return page.map(
                content ->
                        boothContentConverter.toResponse(
                                content,
                                filesByContentId.getOrDefault(content.getId(), List.of()),
                                linksByContentId.getOrDefault(content.getId(), List.of())));
    }

    /** 부스 콘텐츠 상세 조회. */
    @Transactional(readOnly = true)
    public BoothContentResponse get(Long contentId) {
        return toResponseWithFiles(getEntity(contentId));
    }

    /** 운영 확인 처리. 상태는 바꾸지 않고 확인 시각·주체만 기록한다. */
    @Transactional
    public BoothContentResponse check(Long contentId, Long adminId) {
        BoothContent content = getEntity(contentId);
        content.check(adminId);
        return toResponseWithFiles(content);
    }

    /**
     * 검수 승인. 검수 요청 상태에서만 승인할 수 있고, 승인해야 비로소 공개된다.
     *
     * <p>검수 요청 이후 승인 사이에 배정이 취소될 수 있어, 승인 시점에도 배정 상태를 다시 확인한다.
     */
    @Transactional
    public BoothContentResponse approve(Long contentId, Long adminId) {
        BoothContent content = getEntity(contentId);
        if (content.getStatus() != BoothContentStatus.UNDER_REVIEW) {
            throw new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_APPROVABLE);
        }
        assertAllocationAssigned(content);
        content.approve(adminId);
        boothManagementHistoryRepository.save(
                BoothManagementHistory.create(
                        content.getBoothAllocationId(),
                        content.getId(),
                        BoothManagementActionType.CONTENT_APPROVED,
                        null,
                        adminId));
        return toResponseWithFiles(content);
    }

    /** 보완 요청. 검수 중이거나 이미 공개된 콘텐츠만 보완을 요청할 수 있다. 공개 상태였다면 공개를 내린다. */
    @Transactional
    public BoothContentResponse requestCorrection(Long contentId, Long adminId, String message) {
        BoothContent content = getEntity(contentId);
        if (content.getStatus() != BoothContentStatus.UNDER_REVIEW
                && content.getStatus() != BoothContentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_CORRECTION_REQUESTABLE);
        }
        content.requestCorrection(message);
        boothManagementHistoryRepository.save(
                BoothManagementHistory.create(
                        content.getBoothAllocationId(),
                        content.getId(),
                        BoothManagementActionType.CORRECTION_REQUESTED,
                        message,
                        adminId));
        return toResponseWithFiles(content);
    }

    /** 관리자 직권 숨김. 공개되었거나 보완 요청 상태인 콘텐츠만 숨길 수 있다. */
    @Transactional
    public BoothContentResponse hide(Long contentId, Long adminId, String reason) {
        BoothContent content = getEntity(contentId);
        if (content.getStatus() != BoothContentStatus.PUBLISHED
                && content.getStatus() != BoothContentStatus.CORRECTION_REQUESTED) {
            throw new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_HIDABLE);
        }
        content.hide();
        boothManagementHistoryRepository.save(
                BoothManagementHistory.create(
                        content.getBoothAllocationId(),
                        content.getId(),
                        BoothManagementActionType.CONTENT_HIDDEN,
                        reason,
                        adminId));
        return toResponseWithFiles(content);
    }

    /** 숨김 해제. 공개 상태로 복원하고 이력을 남긴다. 배정이 취소된 부스는 복원할 수 없다. */
    @Transactional
    public BoothContentResponse restore(Long contentId, Long adminId, String reason) {
        BoothContent content = getEntity(contentId);
        if (content.getStatus() != BoothContentStatus.HIDDEN) {
            throw new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_RESTORABLE);
        }
        assertAllocationAssigned(content);
        content.restore();
        boothManagementHistoryRepository.save(
                BoothManagementHistory.create(
                        content.getBoothAllocationId(),
                        content.getId(),
                        BoothManagementActionType.CONTENT_RESTORED,
                        reason,
                        adminId));
        return toResponseWithFiles(content);
    }

    /** 콘텐츠별 운영 변경 이력 조회. 최신순. */
    @Transactional(readOnly = true)
    public List<BoothManagementHistoryResponse> listHistory(Long contentId) {
        getEntity(contentId);
        return boothManagementHistoryRepository
                .findAllByBoothContentIdOrderByCreatedAtDescIdDesc(contentId)
                .stream()
                .map(boothManagementHistoryConverter::toResponse)
                .toList();
    }

    private BoothContentResponse toResponseWithFiles(BoothContent content) {
        List<BoothContentFile> files =
                boothContentFileRepository.findAllByBoothContentIdOrderBySortOrderAscIdAsc(
                        content.getId());
        List<ExternalLink> links =
                externalLinkRepository.findAllByBoothContentIdOrderBySortOrderAscIdAsc(
                        content.getId());
        return boothContentConverter.toResponse(content, files, links);
    }

    private BoothContent getEntity(Long contentId) {
        return boothContentRepository
                .findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_FOUND));
    }

    private void assertAllocationAssigned(BoothContent content) {
        BoothAllocation allocation =
                boothAllocationRepository
                        .findById(content.getBoothAllocationId())
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND));
        if (allocation.getStatus() != BoothAllocationStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_ASSIGNED);
        }
    }
}
