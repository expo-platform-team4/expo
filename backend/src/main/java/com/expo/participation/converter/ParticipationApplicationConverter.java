package com.expo.participation.converter;

import com.expo.participation.dto.ParticipationApplicationResponse;
import com.expo.participation.entity.ParticipationApplication;
import org.springframework.stereotype.Component;

@Component
public class ParticipationApplicationConverter {

    public ParticipationApplicationResponse toResponse(ParticipationApplication application) {
        return new ParticipationApplicationResponse(
                application.getId(),
                application.getRecruitmentNoticeId(),
                application.getCompanyNameSnapshot(),
                application.getParticipationPurpose(),
                application.getExhibitDescription(),
                application.getSelectedBoothProductId(),
                application.getBoothOrderId(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
