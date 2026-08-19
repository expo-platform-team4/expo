package com.expo.recruitment.service;

import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.entity.BoothAllocationStatus;
import com.expo.booth.entity.BoothOrder;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothOrderRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.participation.entity.ParticipationApplication;
import com.expo.participation.entity.ParticipationApplicationStatus;
import com.expo.participation.repository.ParticipationApplicationRepository;
import com.expo.recruitment.converter.RecruitmentResultConverter;
import com.expo.recruitment.dto.RecruitmentResultResponse;
import com.expo.recruitment.entity.RecruitmentNotice;
import com.expo.recruitment.entity.RecruitmentNoticeStatus;
import com.expo.recruitment.entity.RecruitmentResult;
import com.expo.recruitment.entity.RecruitmentResultItem;
import com.expo.recruitment.entity.RecruitmentResultStatus;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import com.expo.recruitment.repository.RecruitmentResultItemRepository;
import com.expo.recruitment.repository.RecruitmentResultRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자용 모집 결과 생성·조회·직권 취소. 결과는 생성과 동시에 주최자에게 전달된다. */
@Slf4j
@Service
public class AdminRecruitmentResultService {

    private final RecruitmentNoticeRepository recruitmentNoticeRepository;
    private final RecruitmentResultRepository recruitmentResultRepository;
    private final RecruitmentResultItemRepository recruitmentResultItemRepository;
    private final ParticipationApplicationRepository participationApplicationRepository;
    private final BoothAllocationRepository boothAllocationRepository;
    private final BoothOrderRepository boothOrderRepository;
    private final RecruitmentResultConverter recruitmentResultConverter;

    public AdminRecruitmentResultService(
            RecruitmentNoticeRepository recruitmentNoticeRepository,
            RecruitmentResultRepository recruitmentResultRepository,
            RecruitmentResultItemRepository recruitmentResultItemRepository,
            ParticipationApplicationRepository participationApplicationRepository,
            BoothAllocationRepository boothAllocationRepository,
            BoothOrderRepository boothOrderRepository,
            RecruitmentResultConverter recruitmentResultConverter) {
        this.recruitmentNoticeRepository = recruitmentNoticeRepository;
        this.recruitmentResultRepository = recruitmentResultRepository;
        this.recruitmentResultItemRepository = recruitmentResultItemRepository;
        this.participationApplicationRepository = participationApplicationRepository;
        this.boothAllocationRepository = boothAllocationRepository;
        this.boothOrderRepository = boothOrderRepository;
        this.recruitmentResultConverter = recruitmentResultConverter;
    }

    /**
     * 모집 결과 생성. 마감된 공고의 결제 완료(SUBMITTED) 신청서 중 배정이 살아있는(ASSIGNED) 건만 집계한다.
     *
     * <p>공고당 결과는 하나만 생성할 수 있다. 생성 즉시 주최자에게 전달(DELIVERED)된다.
     */
    @Transactional
    public RecruitmentResultResponse generate(Long recruitmentNoticeId) {
        RecruitmentNotice notice =
                recruitmentNoticeRepository
                        .findById(recruitmentNoticeId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.RECRUITMENT_NOTICE_NOT_FOUND));
        if (notice.getStatus() != RecruitmentNoticeStatus.CLOSED) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_CLOSED);
        }
        if (recruitmentResultRepository.existsByRecruitmentNoticeId(recruitmentNoticeId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RECRUITMENT_RESULT);
        }

        List<ParticipationApplication> submittedApplications =
                participationApplicationRepository.findAllByRecruitmentNoticeIdAndStatus(
                        recruitmentNoticeId, ParticipationApplicationStatus.SUBMITTED);

        RecruitmentResult result;
        try {
            result =
                    recruitmentResultRepository.saveAndFlush(
                            RecruitmentResult.create(
                                    recruitmentNoticeId,
                                    notice.getHostClientId(),
                                    0,
                                    0,
                                    BigDecimal.ZERO));
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("recruitment_results_recruitment_notice_id_key")) {
                throw new BusinessException(ErrorCode.DUPLICATE_RECRUITMENT_RESULT);
            }
            log.warn(
                    "모집 결과 저장 중 예상하지 못한 무결성 제약 위반. recruitmentNoticeId={}", recruitmentNoticeId, e);
            throw e;
        }

        List<RecruitmentResultItem> items = buildItems(result.getId(), submittedApplications);
        recruitmentResultItemRepository.saveAll(items);

        BigDecimal totalAmount =
                items.stream()
                        .map(RecruitmentResultItem::getBoothAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.applyAggregate(items.size(), items.size(), totalAmount);
        result.deliver();

        return recruitmentResultConverter.toResponse(result, items);
    }

    private List<RecruitmentResultItem> buildItems(
            Long recruitmentResultId, List<ParticipationApplication> applications) {
        return applications.stream()
                .map(
                        application ->
                                boothAllocationRepository
                                        .findByApplicationId(application.getId())
                                        .filter(
                                                allocation ->
                                                        allocation.getStatus()
                                                                == BoothAllocationStatus.ASSIGNED)
                                        .map(
                                                allocation ->
                                                        toResultItem(
                                                                recruitmentResultId,
                                                                application,
                                                                allocation)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private RecruitmentResultItem toResultItem(
            Long recruitmentResultId,
            ParticipationApplication application,
            BoothAllocation allocation) {
        BoothOrder order =
                boothOrderRepository
                        .findById(allocation.getBoothOrderId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.BOOTH_ORDER_NOT_FOUND));
        return RecruitmentResultItem.create(
                recruitmentResultId,
                application.getId(),
                application.getClientUserId(),
                allocation.getId(),
                order.getTotalAmount());
    }

    /** 모집 결과 상세 조회. */
    @Transactional(readOnly = true)
    public RecruitmentResultResponse get(Long resultId) {
        RecruitmentResult result = getEntity(resultId);
        return recruitmentResultConverter.toResponse(result, getItems(result.getId()));
    }

    /** 모집 결과 목록 조회 (페이지 단위). */
    @Transactional(readOnly = true)
    public Page<RecruitmentResultResponse> list(Pageable pageable) {
        return recruitmentResultRepository
                .findAll(pageable)
                .map(
                        result ->
                                recruitmentResultConverter.toResponse(
                                        result, getItems(result.getId())));
    }

    /** 관리자 직권 취소. 이미 확정·박람회 반영된 결과는 취소할 수 없다. */
    @Transactional
    public RecruitmentResultResponse cancel(Long resultId) {
        RecruitmentResult result = getEntityForUpdate(resultId);
        if (result.getStatus() == RecruitmentResultStatus.CONFIRMED
                || result.getStatus() == RecruitmentResultStatus.USED_FOR_EXPO) {
            throw new BusinessException(ErrorCode.RECRUITMENT_RESULT_NOT_CANCELABLE);
        }
        result.cancel();
        return recruitmentResultConverter.toResponse(result, getItems(result.getId()));
    }

    private List<RecruitmentResultItem> getItems(Long recruitmentResultId) {
        return recruitmentResultItemRepository.findAllByRecruitmentResultId(recruitmentResultId);
    }

    private RecruitmentResult getEntity(Long resultId) {
        return recruitmentResultRepository
                .findById(resultId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUITMENT_RESULT_NOT_FOUND));
    }

    private RecruitmentResult getEntityForUpdate(Long resultId) {
        return recruitmentResultRepository
                .findByIdForUpdate(resultId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUITMENT_RESULT_NOT_FOUND));
    }
}
