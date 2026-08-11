package com.expo.booth.service;

import com.expo.booth.converter.BoothContentConverter;
import com.expo.booth.converter.BoothManagementHistoryConverter;
import com.expo.booth.dto.BoothContentResponse;
import com.expo.booth.dto.BoothManagementHistoryResponse;
import com.expo.booth.entity.BoothContent;
import com.expo.booth.entity.BoothContentFile;
import com.expo.booth.entity.BoothContentStatus;
import com.expo.booth.entity.BoothManagementActionType;
import com.expo.booth.entity.BoothManagementHistory;
import com.expo.booth.repository.BoothContentFileRepository;
import com.expo.booth.repository.BoothContentRepository;
import com.expo.booth.repository.BoothManagementHistoryRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자용 부스 콘텐츠 조회와 운영 확인·보완 요청·숨김 처리. */
@Service
public class AdminBoothContentService {

    private final BoothContentRepository boothContentRepository;
    private final BoothContentFileRepository boothContentFileRepository;
    private final BoothManagementHistoryRepository boothManagementHistoryRepository;
    private final BoothContentConverter boothContentConverter;
    private final BoothManagementHistoryConverter boothManagementHistoryConverter;

    public AdminBoothContentService(
            BoothContentRepository boothContentRepository,
            BoothContentFileRepository boothContentFileRepository,
            BoothManagementHistoryRepository boothManagementHistoryRepository,
            BoothContentConverter boothContentConverter,
            BoothManagementHistoryConverter boothManagementHistoryConverter) {
        this.boothContentRepository = boothContentRepository;
        this.boothContentFileRepository = boothContentFileRepository;
        this.boothManagementHistoryRepository = boothManagementHistoryRepository;
        this.boothContentConverter = boothContentConverter;
        this.boothManagementHistoryConverter = boothManagementHistoryConverter;
    }

    /** 부스 콘텐츠 목록 조회 (페이지 단위). */
    @Transactional(readOnly = true)
    public Page<BoothContentResponse> list(Pageable pageable) {
        return boothContentRepository
                .findAll(pageable)
                .map(content -> boothContentConverter.toResponse(content, List.of()));
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

    /** 보완 요청. 공개 상태를 내리고 사유를 이력으로 남긴다. */
    @Transactional
    public BoothContentResponse requestCorrection(Long contentId, Long adminId, String message) {
        BoothContent content = getEntity(contentId);
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

    /** 관리자 직권 숨김. */
    @Transactional
    public BoothContentResponse hide(Long contentId, Long adminId, String reason) {
        BoothContent content = getEntity(contentId);
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

    /** 숨김 해제. 공개 상태로 복원하고 이력을 남긴다. */
    @Transactional
    public BoothContentResponse restore(Long contentId, Long adminId, String reason) {
        BoothContent content = getEntity(contentId);
        if (content.getStatus() != BoothContentStatus.HIDDEN) {
            throw new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_EDITABLE);
        }
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
        return boothContentConverter.toResponse(content, files);
    }

    private BoothContent getEntity(Long contentId) {
        return boothContentRepository
                .findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_CONTENT_NOT_FOUND));
    }
}
