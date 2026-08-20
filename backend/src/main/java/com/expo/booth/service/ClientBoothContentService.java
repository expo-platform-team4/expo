package com.expo.booth.service;

import com.expo.booth.converter.BoothContentConverter;
import com.expo.booth.converter.BoothContentFileConverter;
import com.expo.booth.converter.ExternalLinkConverter;
import com.expo.booth.dto.AddBoothContentFileRequest;
import com.expo.booth.dto.AddExternalLinkRequest;
import com.expo.booth.dto.BoothContentFileResponse;
import com.expo.booth.dto.BoothContentResponse;
import com.expo.booth.dto.CreateBoothContentRequest;
import com.expo.booth.dto.ExternalLinkResponse;
import com.expo.booth.dto.PublicBoothContentResponse;
import com.expo.booth.dto.UpdateBoothContentRequest;
import com.expo.booth.dto.UpdateExternalLinkRequest;
import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.entity.BoothAllocationStatus;
import com.expo.booth.entity.BoothContent;
import com.expo.booth.entity.BoothContentFile;
import com.expo.booth.entity.BoothContentStatus;
import com.expo.booth.entity.ExternalLink;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothContentFileRepository;
import com.expo.booth.repository.BoothContentRepository;
import com.expo.booth.repository.ExternalLinkRepository;
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
    private final ExternalLinkRepository externalLinkRepository;
    private final BoothAllocationRepository boothAllocationRepository;
    private final BoothContentConverter boothContentConverter;
    private final BoothContentFileConverter boothContentFileConverter;
    private final ExternalLinkConverter externalLinkConverter;

    public ClientBoothContentService(
            BoothContentRepository boothContentRepository,
            BoothContentFileRepository boothContentFileRepository,
            ExternalLinkRepository externalLinkRepository,
            BoothAllocationRepository boothAllocationRepository,
            BoothContentConverter boothContentConverter,
            BoothContentFileConverter boothContentFileConverter,
            ExternalLinkConverter externalLinkConverter) {
        this.boothContentRepository = boothContentRepository;
        this.boothContentFileRepository = boothContentFileRepository;
        this.externalLinkRepository = externalLinkRepository;
        this.boothAllocationRepository = boothAllocationRepository;
        this.boothContentConverter = boothContentConverter;
        this.boothContentFileConverter = boothContentFileConverter;
        this.externalLinkConverter = externalLinkConverter;
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
            throw new BusinessException(ErrorCode.BOOTH_ALLOCATION_NOT_ASSIGNED);
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
            return boothContentConverter.toResponse(saved, List.of(), List.of());
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

    /** 배정 ID로 내 부스 콘텐츠 조회. 상태와 무관하게(초안 포함) 작성 중인 콘텐츠를 다시 찾을 때 쓴다. */
    @Transactional(readOnly = true)
    public BoothContentResponse getMineByAllocation(Long boothAllocationId, Long clientUserId) {
        BoothContent content =
                boothContentRepository
                        .findByBoothAllocationIdAndClientUserId(boothAllocationId, clientUserId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_FOUND));
        return toResponseWithFiles(content);
    }

    /** 공개된 부스 콘텐츠 조회. 참여를 검토하는 방문자가 배정 ID로 조회한다. 내부 필드는 뺀 응답을 돌려준다. */
    @Transactional(readOnly = true)
    public PublicBoothContentResponse getPublished(Long boothAllocationId) {
        BoothContent content =
                boothContentRepository
                        .findByBoothAllocationId(boothAllocationId)
                        .filter(c -> c.getStatus() == BoothContentStatus.PUBLISHED)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_FOUND));
        List<BoothContentFile> files =
                boothContentFileRepository.findAllByBoothContentIdOrderBySortOrderAscIdAsc(
                        content.getId());
        List<ExternalLink> links =
                externalLinkRepository.findAllByBoothContentIdOrderBySortOrderAscIdAsc(
                        content.getId());
        return boothContentConverter.toPublicResponse(content, files, links);
    }

    /** 콘텐츠 본문 수정. 초안·보완 요청 상태에서만 수정할 수 있다. */
    @Transactional
    public BoothContentResponse update(
            Long contentId, UpdateBoothContentRequest request, Long clientUserId) {
        BoothContent content = getEditableOwnedEntity(contentId, clientUserId);
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

    /** 콘텐츠 검수 요청. 초안·보완 요청 상태에서만 요청할 수 있다. 관리자가 승인해야 실제로 공개된다. */
    @Transactional
    public BoothContentResponse submitForReview(Long contentId, Long clientUserId) {
        BoothContent content = getOwnedEntity(contentId, clientUserId);
        if (!EDITABLE_STATUSES.contains(content.getStatus())) {
            throw new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_SUBMITTABLE);
        }
        content.submitForReview();
        return toResponseWithFiles(content);
    }

    /** 첨부 파일 등록. */
    @Transactional
    public BoothContentFileResponse addFile(
            Long contentId, AddBoothContentFileRequest request, Long clientUserId) {
        BoothContent content = getEditableOwnedEntity(contentId, clientUserId);
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
        getEditableOwnedEntity(contentId, clientUserId);
        BoothContentFile file = getOwnedFile(contentId, fileEntryId);
        boothContentFileRepository.delete(file);
    }

    /** 첨부 파일 노출 순서 변경. */
    @Transactional
    public BoothContentFileResponse reorderFile(
            Long contentId, Long fileEntryId, int sortOrder, Long clientUserId) {
        getEditableOwnedEntity(contentId, clientUserId);
        BoothContentFile file = getOwnedFile(contentId, fileEntryId);
        file.changeSortOrder(sortOrder);
        return boothContentFileConverter.toResponse(file);
    }

    private BoothContentFile getOwnedFile(Long contentId, Long fileEntryId) {
        return boothContentFileRepository
                .findByIdAndBoothContentId(fileEntryId, contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_CONTENT_FILE_NOT_FOUND));
    }

    /** 외부 링크 등록. */
    @Transactional
    public ExternalLinkResponse addLink(
            Long contentId, AddExternalLinkRequest request, Long clientUserId) {
        BoothContent content = getEditableOwnedEntity(contentId, clientUserId);
        ExternalLink link =
                ExternalLink.createForBoothContent(
                        content.getId(),
                        request.linkType(),
                        request.label(),
                        request.url(),
                        request.sortOrder());
        ExternalLink saved = externalLinkRepository.saveAndFlush(link);
        return externalLinkConverter.toResponse(saved);
    }

    /** 외부 링크 수정. */
    @Transactional
    public ExternalLinkResponse updateLink(
            Long contentId, Long linkId, UpdateExternalLinkRequest request, Long clientUserId) {
        getEditableOwnedEntity(contentId, clientUserId);
        ExternalLink link = getOwnedLink(contentId, linkId);
        link.update(request.linkType(), request.label(), request.url());
        return externalLinkConverter.toResponse(link);
    }

    /** 외부 링크 삭제. */
    @Transactional
    public void removeLink(Long contentId, Long linkId, Long clientUserId) {
        getEditableOwnedEntity(contentId, clientUserId);
        ExternalLink link = getOwnedLink(contentId, linkId);
        externalLinkRepository.delete(link);
    }

    /** 외부 링크 노출 순서 변경. */
    @Transactional
    public ExternalLinkResponse reorderLink(
            Long contentId, Long linkId, int sortOrder, Long clientUserId) {
        getEditableOwnedEntity(contentId, clientUserId);
        ExternalLink link = getOwnedLink(contentId, linkId);
        link.changeSortOrder(sortOrder);
        return externalLinkConverter.toResponse(link);
    }

    private ExternalLink getOwnedLink(Long contentId, Long linkId) {
        return externalLinkRepository
                .findByIdAndBoothContentId(linkId, contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXTERNAL_LINK_NOT_FOUND));
    }

    /** 소유권과 함께 수정 가능 상태(초안·보완 요청)인지 확인한다. */
    private BoothContent getEditableOwnedEntity(Long contentId, Long clientUserId) {
        BoothContent content = getOwnedEntity(contentId, clientUserId);
        if (!EDITABLE_STATUSES.contains(content.getStatus())) {
            throw new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_EDITABLE);
        }
        return content;
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

    private BoothContent getOwnedEntity(Long contentId, Long clientUserId) {
        return boothContentRepository
                .findByIdAndClientUserId(contentId, clientUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_FOUND));
    }
}
