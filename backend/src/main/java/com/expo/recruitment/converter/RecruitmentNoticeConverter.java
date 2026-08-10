package com.expo.recruitment.converter;

import com.expo.recruitment.dto.RecruitmentNoticeResponse;
import com.expo.recruitment.entity.RecruitmentNotice;
import org.springframework.stereotype.Component;

@Component
public class RecruitmentNoticeConverter {

    public RecruitmentNoticeResponse toResponse(RecruitmentNotice notice) {
        return new RecruitmentNoticeResponse(
                notice.getId(),
                notice.getRequestId(),
                notice.getHostClientId(),
                notice.getVenueReservationId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getEligibility(),
                notice.getSubmissionRequirements(),
                notice.getApplicationStartAt(),
                notice.getApplicationEndAt(),
                notice.getStatus(),
                notice.getPublishedAt(),
                notice.getClosedAt(),
                notice.getCreatedByAdminId(),
                notice.getCreatedAt(),
                notice.getUpdatedAt());
    }
}
