package com.expo.participation.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.entity.Expo;
import com.expo.expo.repository.ExpoRepository;
import com.expo.participation.converter.ParticipationApplicationConverter;
import com.expo.participation.dto.ClientParticipatingCompanyResponse;
import com.expo.participation.repository.ParticipationApplicationRepository;
import com.expo.recruitment.entity.RecruitmentNotice;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주최사(박람회 개설 클라이언트)가 자기 박람회에 참여 신청한 기업 목록을 조회한다.
 *
 * <p>{@code expos} 에는 참여 기업을 직접 잇는 컬럼이 없다 — 실제 연결 경로는
 * {@code expos} → {@code recruitment_notices}(expo_id) → {@code participation_applications}
 * (recruitment_notice_id) 다. 박람회 하나에 모집공고가 여러 건일 수 있어(재모집 등) 공고
 * ID 목록을 먼저 구하고, 그 공고들에 달린 신청서를 한 번에 모아 온다.
 */
@Service
public class ClientParticipatingCompanyService {

    private final ExpoRepository expoRepository;
    private final RecruitmentNoticeRepository recruitmentNoticeRepository;
    private final ParticipationApplicationRepository participationApplicationRepository;
    private final ParticipationApplicationConverter participationApplicationConverter;

    public ClientParticipatingCompanyService(
            ExpoRepository expoRepository,
            RecruitmentNoticeRepository recruitmentNoticeRepository,
            ParticipationApplicationRepository participationApplicationRepository,
            ParticipationApplicationConverter participationApplicationConverter) {
        this.expoRepository = expoRepository;
        this.recruitmentNoticeRepository = recruitmentNoticeRepository;
        this.participationApplicationRepository = participationApplicationRepository;
        this.participationApplicationConverter = participationApplicationConverter;
    }

    /** 참여 기업 목록 조회. 신청 취소(CANCELED) 건도 포함한다 — 취소 이력도 주최사에게는 의미가 있다. */
    @Transactional(readOnly = true)
    public List<ClientParticipatingCompanyResponse> list(Long expoId, Long hostClientUserId) {
        requireHost(expoId, hostClientUserId);

        List<Long> noticeIds =
                recruitmentNoticeRepository.findAllByExpoId(expoId).stream()
                        .map(RecruitmentNotice::getId)
                        .toList();
        if (noticeIds.isEmpty()) {
            return List.of();
        }

        return participationApplicationRepository.findAllByRecruitmentNoticeIdIn(noticeIds).stream()
                .map(participationApplicationConverter::toClientResponse)
                .toList();
    }

    private void requireHost(Long expoId, Long clientUserId) {
        Expo expo =
                expoRepository
                        .findById(expoId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.EXPO_NOT_FOUND));
        if (!expo.getHostClientId().equals(clientUserId)) {
            throw new BusinessException(ErrorCode.NOT_EXPO_HOST);
        }
    }
}
