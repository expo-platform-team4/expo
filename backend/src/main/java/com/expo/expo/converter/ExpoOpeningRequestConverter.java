package com.expo.expo.converter;

import com.expo.expo.dto.ExpoOpeningRequestResponse;
import com.expo.expo.entity.ExpoOpeningRequest;
import org.springframework.stereotype.Component;

/** 개최 신청 엔티티 → 응답 DTO. 회사명·장소명·생성된 박람회 ID 는 서비스가 조회해 넘긴다. */
@Component
public class ExpoOpeningRequestConverter {

    public ExpoOpeningRequestResponse toResponse(
            ExpoOpeningRequest request,
            String hostCompanyName,
            String desiredVenueName,
            Long createdExpoId) {
        return new ExpoOpeningRequestResponse(
                request.getId(),
                request.getHostClientId(),
                hostCompanyName,
                request.getTitle(),
                request.getDescription(),
                request.getEventStartAt(),
                request.getEventEndAt(),
                request.getSalesStartAt(),
                request.getSalesEndAt(),
                request.getDesiredVenueId(),
                desiredVenueName,
                request.getDesiredVenueHallId(),
                request.getDesiredVenueZoneId(),
                request.getStatus(),
                request.getSubmittedAt(),
                request.getReviewedByAdminId(),
                request.getReviewedAt(),
                request.getRejectionReason(),
                createdExpoId,
                request.getCreatedAt());
    }
}
