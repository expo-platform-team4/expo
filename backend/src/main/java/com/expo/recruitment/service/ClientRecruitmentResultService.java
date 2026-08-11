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

/** 주최자용 모집 결과 조회·확인. */
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

    /** 내 모집 결과 상세 조회. */
    @Transactional(readOnly = true)
    public RecruitmentResultResponse getMine(Long resultId, Long hostClientId) {
        RecruitmentResult result = getOwnedEntity(resultId, hostClientId);
        return recruitmentResultConverter.toResponse(result, getItems(result.getId()));
    }

    /** 모집 결과 확인. 전달(DELIVERED) 상태에서만 확인할 수 있다. */
    @Transactional
    public RecruitmentResultResponse confirm(Long resultId, Long hostClientId) {
        RecruitmentResult result = getOwnedEntity(resultId, hostClientId);
        if (result.getStatus() != RecruitmentResultStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.RECRUITMENT_RESULT_NOT_CONFIRMABLE);
        }
        result.confirmByHost();
        return recruitmentResultConverter.toResponse(result, getItems(result.getId()));
    }

    private List<RecruitmentResultItem> getItems(Long recruitmentResultId) {
        return recruitmentResultItemRepository.findAllByRecruitmentResultId(recruitmentResultId);
    }

    private RecruitmentResult getOwnedEntity(Long resultId, Long hostClientId) {
        return recruitmentResultRepository
                .findByIdAndHostClientId(resultId, hostClientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUITMENT_RESULT_NOT_FOUND));
    }
}
