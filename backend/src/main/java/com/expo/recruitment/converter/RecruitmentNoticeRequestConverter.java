package com.expo.recruitment.converter;

import com.expo.recruitment.dto.RecruitmentNoticeRequestResponse;
import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecruitmentNoticeRequestConverter {

    /**
     * @param venueReservationConfirmed 장소 예약이 확정되어 있는지
     * @param noticeCreated 이 요청으로 만든 공고가 이미 있는지
     */
    public RecruitmentNoticeRequestResponse toResponse(
            RecruitmentNoticeRequest request,
            List<Long> venueZoneIds,
            boolean venueReservationConfirmed,
            boolean noticeCreated) {
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
                request.getUpdatedAt(),
                venueReservationConfirmed,
                noticeCreated);
    }
}
