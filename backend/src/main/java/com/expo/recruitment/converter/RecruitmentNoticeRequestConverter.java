package com.expo.recruitment.converter;

import com.expo.recruitment.dto.RecruitmentNoticeRequestResponse;
import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecruitmentNoticeRequestConverter {

    public RecruitmentNoticeRequestResponse toResponse(
            RecruitmentNoticeRequest request, List<Long> venueZoneIds) {
        return new RecruitmentNoticeRequestResponse(
                request.getId(),
                request.getHostClientId(),
                request.getExpoId(),
                request.getTitle(),
                request.getDescription(),
                request.getApplicationStartAt(),
                request.getApplicationEndAt(),
                request.getEventStartAt(),
                request.getEventEndAt(),
                request.getVirtualVenueId(),
                request.getVenueHallId(),
                venueZoneIds,
                request.getTargetCompanyCount(),
                request.getRequestedBoothConfig(),
                request.getStatus(),
                request.getVenueConflictStatus(),
                request.getVenueDecision(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
