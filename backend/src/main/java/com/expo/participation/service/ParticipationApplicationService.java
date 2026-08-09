package com.expo.participation.service;

import com.expo.booth.repository.BoothProductRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.participation.converter.ParticipationApplicationConverter;
import com.expo.participation.dto.CreateParticipationApplicationRequest;
import com.expo.participation.dto.ParticipationApplicationResponse;
import com.expo.participation.entity.ParticipationApplication;
import com.expo.participation.repository.ParticipationApplicationRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipationApplicationService {

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

    /** 참여 신청서 작성. 모집공고와 선택한 부스 상품이 실제로 존재하는지 검증한다. */
    @Transactional
    public ParticipationApplicationResponse create(
            Long clientUserId, CreateParticipationApplicationRequest request) {
        if (!recruitmentNoticeRepository.existsById(request.recruitmentNoticeId())) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_FOUND);
        }
        if (request.selectedBoothProductId() != null
                && !boothProductRepository.existsById(request.selectedBoothProductId())) {
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
        ParticipationApplication saved = participationApplicationRepository.save(application);
        return participationApplicationConverter.toResponse(saved);
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
}
