package com.expo.recruitment.converter;

import com.expo.recruitment.dto.RecruitmentResultResponse;
import com.expo.recruitment.entity.RecruitmentResult;
import com.expo.recruitment.entity.RecruitmentResultItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecruitmentResultConverter {

    private final RecruitmentResultItemConverter recruitmentResultItemConverter;

    public RecruitmentResultConverter(
            RecruitmentResultItemConverter recruitmentResultItemConverter) {
        this.recruitmentResultItemConverter = recruitmentResultItemConverter;
    }

    public RecruitmentResultResponse toResponse(
            RecruitmentResult result, List<RecruitmentResultItem> items) {
        return new RecruitmentResultResponse(
                result.getId(),
                result.getRecruitmentNoticeId(),
                result.getHostClientId(),
                result.getConfirmedCompanyCount(),
                result.getConfirmedBoothCount(),
                result.getTotalBoothSalesAmount(),
                result.getStatus(),
                result.getGeneratedAt(),
                result.getDeliveredAt(),
                result.getConfirmedByHostAt(),
                items.stream().map(recruitmentResultItemConverter::toResponse).toList(),
                result.getCreatedAt());
    }
}
