package com.expo.participation.service;

import com.expo.booth.repository.BoothProductRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.participation.converter.ParticipationApplicationConverter;
import com.expo.participation.dto.CreateParticipationApplicationRequest;
import com.expo.participation.dto.ParticipationApplicationResponse;
import com.expo.participation.dto.UpdateParticipationApplicationRequest;
import com.expo.participation.entity.ParticipationApplication;
import com.expo.participation.entity.ParticipationApplicationStatus;
import com.expo.participation.repository.ParticipationApplicationRepository;
import com.expo.recruitment.entity.RecruitmentNotice;
import com.expo.recruitment.entity.RecruitmentNoticeStatus;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import java.util.EnumSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ParticipationApplicationService {

    private static final EnumSet<ParticipationApplicationStatus> ACTIVE_STATUSES =
            EnumSet.of(
                    ParticipationApplicationStatus.DRAFT,
                    ParticipationApplicationStatus.PAYMENT_PENDING,
                    ParticipationApplicationStatus.SUBMITTED);

    private final ParticipationApplicationRepository participationApplicationRepository;
    private final RecruitmentNoticeRepository recruitmentNoticeRepository;
    private final BoothProductRepository boothProductRepository;
    private final ParticipationApplicationConverter participationApplicationConverter;

    public ParticipationApplicationService(
            ParticipationApplicationRepository participationApplicationRepository,
            RecruitmentNoticeRepository recruitmentNoticeRepository,
            BoothProductRepository boothProductRepository,
            ParticipationApplicationConverter participationApplicationConverter) {
        this.participationApplicationRepository = participationApplicationRepository;
        this.recruitmentNoticeRepository = recruitmentNoticeRepository;
        this.boothProductRepository = boothProductRepository;
        this.participationApplicationConverter = participationApplicationConverter;
    }

    /** 참여 신청서 작성. 모집공고가 게시 중인지, 선택한 부스 상품이 실제로 존재하는지 검증한다. */
    @Transactional
    public ParticipationApplicationResponse create(
            Long clientUserId, CreateParticipationApplicationRequest request) {
        RecruitmentNotice notice =
                recruitmentNoticeRepository
                        .findById(request.recruitmentNoticeId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.RECRUITMENT_NOTICE_NOT_FOUND));
        if (notice.getStatus() != RecruitmentNoticeStatus.OPEN) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_OPEN);
        }
        if (clientUserId.equals(notice.getHostClientId())) {
            throw new BusinessException(ErrorCode.CANNOT_APPLY_TO_OWN_NOTICE);
        }
        if (participationApplicationRepository
                .existsByRecruitmentNoticeIdAndClientUserIdAndStatusIn(
                        request.recruitmentNoticeId(), clientUserId, ACTIVE_STATUSES)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PARTICIPATION_APPLICATION);
        }
        if (request.selectedBoothProductId() != null
                && !boothProductRepository.existsByIdAndRecruitmentNoticeId(
                        request.selectedBoothProductId(), notice.getId())) {
            throw new BusinessException(ErrorCode.BOOTH_PRODUCT_NOT_FOUND);
        }
        ParticipationApplication application =
                ParticipationApplication.create(
                        request.recruitmentNoticeId(),
                        clientUserId,
                        request.companyNameSnapshot(),
                        request.participationPurpose(),
                        request.exhibitDescription(),
                        request.selectedBoothProductId());
        try {
            ParticipationApplication saved =
                    participationApplicationRepository.saveAndFlush(application);
            return participationApplicationConverter.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("uq_participation_applications_active_client")) {
                throw new BusinessException(ErrorCode.DUPLICATE_PARTICIPATION_APPLICATION);
            }
            log.warn(
                    "참여 신청서 저장 중 예상하지 못한 무결성 제약 위반. recruitmentNoticeId={}, clientUserId={}",
                    request.recruitmentNoticeId(),
                    clientUserId,
                    e);
            throw e;
        }
    }

    /** 본인이 작성한 참여 신청서 상세 조회. */
    @Transactional(readOnly = true)
    public ParticipationApplicationResponse getMine(Long applicationId, Long clientUserId) {
        ParticipationApplication application =
                participationApplicationRepository
                        .findByIdAndClientUserId(applicationId, clientUserId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.PARTICIPATION_APPLICATION_NOT_FOUND));
        return participationApplicationConverter.toResponse(application);
    }

    /** 참여 신청서 초안 수정. 초안 상태에서만 가능하다 - 결제가 시작된 뒤에는 주문을 먼저 취소해야 한다. */
    @Transactional
    public ParticipationApplicationResponse update(
            Long applicationId, Long clientUserId, UpdateParticipationApplicationRequest request) {
        ParticipationApplication application = getOwnedDraft(applicationId, clientUserId);
        if (request.selectedBoothProductId() != null
                && !boothProductRepository.existsByIdAndRecruitmentNoticeId(
                        request.selectedBoothProductId(), application.getRecruitmentNoticeId())) {
            throw new BusinessException(ErrorCode.BOOTH_PRODUCT_NOT_FOUND);
        }
        application.update(
                request.companyNameSnapshot(),
                request.participationPurpose(),
                request.exhibitDescription(),
                request.selectedBoothProductId());
        return participationApplicationConverter.toResponse(application);
    }

    /** 참여 신청 철회. 초안 상태에서만 가능하다 - 결제가 시작된 뒤에는 주문 취소·환불 흐름을 거쳐야 한다. */
    @Transactional
    public ParticipationApplicationResponse withdraw(Long applicationId, Long clientUserId) {
        ParticipationApplication application = getOwnedDraft(applicationId, clientUserId);
        application.cancel();
        return participationApplicationConverter.toResponse(application);
    }

    private ParticipationApplication getOwnedDraft(Long applicationId, Long clientUserId) {
        ParticipationApplication application =
                participationApplicationRepository
                        .findByIdAndClientUserId(applicationId, clientUserId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.PARTICIPATION_APPLICATION_NOT_FOUND));
        if (application.getStatus() != ParticipationApplicationStatus.DRAFT) {
            throw new BusinessException(ErrorCode.PARTICIPATION_APPLICATION_NOT_EDITABLE);
        }
        return application;
    }
}
