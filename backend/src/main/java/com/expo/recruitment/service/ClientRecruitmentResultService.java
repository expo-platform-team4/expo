package com.expo.recruitment.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.recruitment.converter.RecruitmentResultConverter;
import com.expo.recruitment.dto.RecruitmentResultResponse;
import com.expo.recruitment.entity.RecruitmentResult;
import com.expo.recruitment.entity.RecruitmentResultItem;
import com.expo.recruitment.entity.RecruitmentResultStatus;
import com.expo.recruitment.repository.RecruitmentResultItemRepository;
import com.expo.recruitment.repository.RecruitmentResultRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 주최자용 모집 결과 조회. 전달된 결과를 조회하면 그 즉시 확인 처리된다. */
@Service
public class ClientRecruitmentResultService {

    private final RecruitmentResultRepository recruitmentResultRepository;
    private final RecruitmentResultItemRepository recruitmentResultItemRepository;
    private final RecruitmentResultConverter recruitmentResultConverter;

    public ClientRecruitmentResultService(
            RecruitmentResultRepository recruitmentResultRepository,
            RecruitmentResultItemRepository recruitmentResultItemRepository,
            RecruitmentResultConverter recruitmentResultConverter) {
        this.recruitmentResultRepository = recruitmentResultRepository;
        this.recruitmentResultItemRepository = recruitmentResultItemRepository;
        this.recruitmentResultConverter = recruitmentResultConverter;
    }

    /**
     * 내 모집 결과 상세 조회. 전달(DELIVERED) 상태의 결과를 조회하면 그 자리에서 확인(CONFIRMED) 처리된다.
     *
     * <p>조회와 그 결과로 확인 처리하는 것 사이에 행 잠금을 걸어, 동시에 들어온 두 조회 요청이 같은 전달 상태를 보고 둘 다
     * 확인 처리를 중복 수행하지 못하게 막는다.
     */
    @Transactional
    public RecruitmentResultResponse getMine(Long resultId, Long hostClientId) {
        RecruitmentResult result =
                recruitmentResultRepository
                        .findByIdAndHostClientIdForUpdate(resultId, hostClientId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.RECRUITMENT_RESULT_NOT_FOUND));
        if (result.getStatus() == RecruitmentResultStatus.DELIVERED) {
            result.confirmByHost();
        }
        return recruitmentResultConverter.toResponse(result, getItems(result.getId()));
    }

    private List<RecruitmentResultItem> getItems(Long recruitmentResultId) {
        return recruitmentResultItemRepository.findAllByRecruitmentResultId(recruitmentResultId);
    }
}
