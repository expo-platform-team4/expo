package com.expo.participation.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.participation.converter.ApplicationOperationHistoryConverter;
import com.expo.participation.converter.ParticipationApplicationConverter;
import com.expo.participation.dto.AdminParticipationApplicationResponse;
import com.expo.participation.dto.ApplicationOperationHistoryResponse;
import com.expo.participation.entity.ApplicationOperationActionType;
import com.expo.participation.entity.ApplicationOperationHistory;
import com.expo.participation.entity.ParticipationApplication;
import com.expo.participation.repository.ApplicationOperationHistoryRepository;
import com.expo.participation.repository.ParticipationApplicationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자용 참여 신청서 조회와 운영 확인·보완 요청 처리. 승인·반려 모델은 사용하지 않는다. */
@Service
public class AdminParticipationApplicationService {

    private final ParticipationApplicationRepository participationApplicationRepository;
    private final ApplicationOperationHistoryRepository applicationOperationHistoryRepository;
    private final ParticipationApplicationConverter participationApplicationConverter;
    private final ApplicationOperationHistoryConverter applicationOperationHistoryConverter;

    public AdminParticipationApplicationService(
            ParticipationApplicationRepository participationApplicationRepository,
            ApplicationOperationHistoryRepository applicationOperationHistoryRepository,
            ParticipationApplicationConverter participationApplicationConverter,
            ApplicationOperationHistoryConverter applicationOperationHistoryConverter) {
        this.participationApplicationRepository = participationApplicationRepository;
        this.applicationOperationHistoryRepository = applicationOperationHistoryRepository;
        this.participationApplicationConverter = participationApplicationConverter;
        this.applicationOperationHistoryConverter = applicationOperationHistoryConverter;
    }

    /** 참여 신청서 목록 조회. 모집공고 ID를 지정하면 해당 공고의 신청서만 조회한다. */
    @Transactional(readOnly = true)
    public List<AdminParticipationApplicationResponse> list(Long recruitmentNoticeId) {
        List<ParticipationApplication> applications =
                recruitmentNoticeId != null
                        ? participationApplicationRepository.findAllByRecruitmentNoticeId(
                                recruitmentNoticeId)
                        : participationApplicationRepository.findAll();
        return applications.stream()
                .map(participationApplicationConverter::toAdminResponse)
                .toList();
    }

    /** 참여 신청서 상세 조회. */
    @Transactional(readOnly = true)
    public AdminParticipationApplicationResponse get(Long applicationId) {
        return participationApplicationConverter.toAdminResponse(getEntity(applicationId));
    }

    /** 운영 확인 처리. 확인 시각·주체를 기록하고 이력을 남긴다. */
    @Transactional
    public AdminParticipationApplicationResponse check(
            Long applicationId, Long adminId, String message) {
        ParticipationApplication application = getEntity(applicationId);
        application.check(adminId);
        applicationOperationHistoryRepository.save(
                ApplicationOperationHistory.create(
                        applicationId, ApplicationOperationActionType.CHECKED, message, adminId));
        return participationApplicationConverter.toAdminResponse(application);
    }

    /** 보완 요청. 신청 기업에게 전달할 사유를 이력으로 남긴다. */
    @Transactional
    public AdminParticipationApplicationResponse requestCorrection(
            Long applicationId, Long adminId, String message) {
        getEntity(applicationId);
        applicationOperationHistoryRepository.save(
                ApplicationOperationHistory.create(
                        applicationId,
                        ApplicationOperationActionType.CORRECTION_REQUESTED,
                        message,
                        adminId));
        return participationApplicationConverter.toAdminResponse(getEntity(applicationId));
    }

    /**
     * 보완 완료 처리. 가장 최근 이력이 보완 요청이어야 한다.
     *
     * <p>이력 조회와 그 결과로 완료 이력을 남기는 것 사이에 신청서 행 잠금을 걸어, 동시에 들어온 두 완료 요청이 같은 보완 요청
     * 이력을 보고 둘 다 통과하지 못하게 막는다.
     */
    @Transactional
    public AdminParticipationApplicationResponse completeCorrection(
            Long applicationId, Long adminId, String message) {
        ParticipationApplication application =
                participationApplicationRepository
                        .findByIdForUpdate(applicationId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.PARTICIPATION_APPLICATION_NOT_FOUND));
        ApplicationOperationHistory latest =
                applicationOperationHistoryRepository
                        .findFirstByApplicationIdOrderByCreatedAtDescIdDesc(applicationId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.CORRECTION_NOT_REQUESTED));
        if (latest.getActionType() != ApplicationOperationActionType.CORRECTION_REQUESTED) {
            throw new BusinessException(ErrorCode.CORRECTION_NOT_REQUESTED);
        }
        applicationOperationHistoryRepository.save(
                ApplicationOperationHistory.create(
                        applicationId,
                        ApplicationOperationActionType.CORRECTION_COMPLETED,
                        message,
                        adminId));
        return participationApplicationConverter.toAdminResponse(application);
    }

    /** 관리자 메모 갱신. */
    @Transactional
    public AdminParticipationApplicationResponse updateMemo(
            Long applicationId, Long adminId, String memo) {
        ParticipationApplication application = getEntity(applicationId);
        application.updateMemo(memo);
        applicationOperationHistoryRepository.save(
                ApplicationOperationHistory.create(
                        applicationId, ApplicationOperationActionType.MEMO_UPDATED, memo, adminId));
        return participationApplicationConverter.toAdminResponse(application);
    }

    /** 신청서별 운영 확인·보완 요청 이력 조회. 최신순. */
    @Transactional(readOnly = true)
    public List<ApplicationOperationHistoryResponse> listHistory(Long applicationId) {
        getEntity(applicationId);
        return applicationOperationHistoryRepository
                .findAllByApplicationIdOrderByCreatedAtDescIdDesc(applicationId)
                .stream()
                .map(applicationOperationHistoryConverter::toResponse)
                .toList();
    }

    private ParticipationApplication getEntity(Long applicationId) {
        return participationApplicationRepository
                .findById(applicationId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.PARTICIPATION_APPLICATION_NOT_FOUND));
    }
}
