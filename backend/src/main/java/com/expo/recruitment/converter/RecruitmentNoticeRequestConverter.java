package com.expo.recruitment.converter;

import com.expo.recruitment.dto.RecruitmentNoticeRequestResponse;
import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import org.springframework.stereotype.Component;

@Component
public class RecruitmentNoticeRequestConverter {

    public RecruitmentNoticeRequestResponse toResponse(RecruitmentNoticeRequest request) {
        return new RecruitmentNoticeRequestResponse(
                request.getId(),
                request.getHostClientId(),
                request.getTitle(),
                request.getDescription(),
                request.getApplicationStartAt(),
                request.getApplicationEndAt(),
                request.getEventStartAt(),
                request.getEventEndAt(),
                request.getVirtualVenueId(),
                request.getVenueHallId(),
                request.getVenueZoneId(),
                request.getTargetCompanyCount(),
                request.getRequestedBoothConfig(),
                request.getStatus(),
                request.getVenueConflictStatus(),
                request.getVenueDecision(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
