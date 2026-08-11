package com.expo.booth.service;

import com.expo.booth.converter.BoothContentConverter;
import com.expo.booth.converter.BoothContentFileConverter;
import com.expo.booth.dto.AddBoothContentFileRequest;
import com.expo.booth.dto.BoothContentFileResponse;
import com.expo.booth.dto.BoothContentResponse;
import com.expo.booth.dto.CreateBoothContentRequest;
import com.expo.booth.dto.UpdateBoothContentRequest;
import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.entity.BoothAllocationStatus;
import com.expo.booth.entity.BoothContent;
import com.expo.booth.entity.BoothContentFile;
import com.expo.booth.entity.BoothContentStatus;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothContentFileRepository;
import com.expo.booth.repository.BoothContentRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 참여 기업의 부스 콘텐츠 작성·수정·공개와 첨부 파일 관리. */
@Slf4j
@Service
public class ClientBoothContentService {

    private static final Set<BoothContentStatus> EDITABLE_STATUSES =
            EnumSet.of(BoothContentStatus.DRAFT, BoothContentStatus.CORRECTION_REQUESTED);

    private final BoothContentRepository boothContentRepository;
    private final BoothContentFileRepository boothContentFileRepository;
    private final BoothAllocationRepository boothAllocationRepository;
    private final BoothContentConverter boothContentConverter;
    private final BoothContentFileConverter boothContentFileConverter;

    public ClientBoothContentService(
            BoothContentRepository boothContentRepository,
            BoothContentFileRepository boothContentFileRepository,
            BoothAllocationRepository boothAllocationRepository,
            BoothContentConverter boothContentConverter,
            BoothContentFileConverter boothContentFileConverter) {
        this.boothContentRepository = boothContentRepository;
        this.boothContentFileRepository = boothContentFileRepository;
        this.boothAllocationRepository = boothAllocationRepository;
        this.boothContentConverter = boothContentConverter;
        this.boothContentFileConverter = boothContentFileConverter;
    }

    /** 부스 콘텐츠 작성. 확정 배정 1건당 콘텐츠는 하나만 가질 수 있다. */
    @Transactional
    public BoothContentResponse create(CreateBoothContentRequest request, Long clientUserId) {
        BoothAllocation allocation =
                boothAllocationRepository
                        .findByIdAndClientUserId(request.boothAllocationId(), clientUserId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND));
        if (allocation.getStatus() != BoothAllocationStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_CANCELABLE);
        }
        BoothContent content =
                BoothContent.create(
                                allocation.getId(),
                                clientUserId,
                                request.companyDisplayName(),
                                request.title(),
                                request.companyDescription(),
                                request.boothDescription(),
                                request.productDescription())
                        .attachImages(request.logoFileId(), request.mainImageFileId());
        try {
            BoothContent saved = boothContentRepository.saveAndFlush(content);
            return boothContentConverter.toResponse(saved, List.of());
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("booth_contents_booth_allocation_id_key")) {
                throw new BusinessException(ErrorCode.DUPLICATE_BOOTH_CONTENT);
            }
            log.warn(
                    "부스 콘텐츠 저장 중 예상하지 못한 무결성 제약 위반. boothAllocationId={}",
                    request.boothAllocationId(),
                    e);
            throw e;
        }
    }

    /** 내 부스 콘텐츠 상세 조회. */
    @Transactional(readOnly = true)
    public BoothContentResponse getMine(Long contentId, Long clientUserId) {
        return toResponseWithFiles(getOwnedEntity(contentId, clientUserId));
    }

    /** 공개된 부스 콘텐츠 조회. 참여를 검토하는 방문자가 배정 ID로 조회한다. */
    @Transactional(readOnly = true)
    public BoothContentResponse getPublished(Long boothAllocationId) {
        BoothContent content =
                boothContentRepository
                        .findByBoothAllocationId(boothAllocationId)
                        .filter(c -> c.getStatus() == BoothContentStatus.PUBLISHED)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_FOUND));
        return toResponseWithFiles(content);
    }

    /** 콘텐츠 본문 수정. 초안·보완 요청 상태에서만 수정할 수 있다. */
    @Transactional
    public BoothContentResponse update(
            Long contentId, UpdateBoothContentRequest request, Long clientUserId) {
        BoothContent content = getOwnedEntity(contentId, clientUserId);
        if (!EDITABLE_STATUSES.contains(content.getStatus())) {
            throw new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_EDITABLE);
        }
        content.updateContent(
                request.companyDisplayName(),
                request.title(),
                request.companyDescription(),
                request.boothDescription(),
                request.productDescription(),
                request.logoFileId(),
                request.mainImageFileId());
        return toResponseWithFiles(content);
    }

    /** 콘텐츠 공개. 초안·보완 요청 상태에서만 공개할 수 있다. */
    @Transactional
    public BoothContentResponse publish(Long contentId, Long clientUserId) {
        BoothContent content = getOwnedEntity(contentId, clientUserId);
        if (!EDITABLE_STATUSES.contains(content.getStatus())) {
            throw new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_PUBLISHABLE);
        }
        content.publish();
        return toResponseWithFiles(content);
    }

    /** 첨부 파일 등록. */
    @Transactional
    public BoothContentFileResponse addFile(
            Long contentId, AddBoothContentFileRequest request, Long clientUserId) {
        BoothContent content = getOwnedEntity(contentId, clientUserId);
        BoothContentFile file =
                BoothContentFile.create(
                        content.getId(),
                        request.fileId(),
                        request.fileType(),
                        request.title(),
                        request.sortOrder());
        try {
            BoothContentFile saved = boothContentFileRepository.saveAndFlush(file);
            return boothContentFileConverter.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("uq_booth_content_files_file")) {
                throw new BusinessException(ErrorCode.DUPLICATE_BOOTH_CONTENT_FILE);
            }
            log.warn("부스 콘텐츠 첨부 파일 저장 중 예상하지 못한 무결성 제약 위반. boothContentId={}", contentId, e);
            throw e;
        }
    }

    /** 첨부 파일 삭제. */
    @Transactional
    public void removeFile(Long contentId, Long fileEntryId, Long clientUserId) {
        getOwnedEntity(contentId, clientUserId);
        BoothContentFile file = getOwnedFile(contentId, fileEntryId);
        boothContentFileRepository.delete(file);
    }

    /** 첨부 파일 노출 순서 변경. */
    @Transactional
    public BoothContentFileResponse reorderFile(
            Long contentId, Long fileEntryId, int sortOrder, Long clientUserId) {
        getOwnedEntity(contentId, clientUserId);
        BoothContentFile file = getOwnedFile(contentId, fileEntryId);
        file.changeSortOrder(sortOrder);
        return boothContentFileConverter.toResponse(file);
    }

    private BoothContentFile getOwnedFile(Long contentId, Long fileEntryId) {
        return boothContentFileRepository
                .findByIdAndBoothContentId(fileEntryId, contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_CONTENT_FILE_NOT_FOUND));
    }

    private BoothContentResponse toResponseWithFiles(BoothContent content) {
        List<BoothContentFile> files =
                boothContentFileRepository.findAllByBoothContentIdOrderBySortOrderAscIdAsc(
                        content.getId());
        return boothContentConverter.toResponse(content, files);
    }

    private BoothContent getOwnedEntity(Long contentId, Long clientUserId) {
        return boothContentRepository
                .findByIdAndClientUserId(contentId, clientUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_FOUND));
    }
}
