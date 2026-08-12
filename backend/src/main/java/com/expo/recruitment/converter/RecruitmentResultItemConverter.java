package com.expo.recruitment.converter;

import com.expo.recruitment.dto.RecruitmentResultItemResponse;
import com.expo.recruitment.entity.RecruitmentResultItem;
import org.springframework.stereotype.Component;

@Component
public class RecruitmentResultItemConverter {

    public RecruitmentResultItemResponse toResponse(RecruitmentResultItem item) {
        return new RecruitmentResultItemResponse(
                item.getId(),
                item.getApplicationId(),
                item.getClientUserId(),
                item.getBoothAllocationId(),
                item.getBoothAmount(),
                item.getCreatedAt());
    }
}
