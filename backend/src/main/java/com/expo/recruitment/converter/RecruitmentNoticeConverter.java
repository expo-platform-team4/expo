package com.expo.recruitment.converter;

import com.expo.recruitment.dto.RecruitmentNoticeResponse;
import com.expo.recruitment.entity.RecruitmentNotice;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecruitmentNoticeConverter {

    public RecruitmentNoticeResponse toResponse(
            RecruitmentNotice notice,
            Long venueHallId,
            List<Long> venueZoneIds,
            Long venueHallLayoutFileId,
            List<Long> venueZoneLayoutFileIds) {
        return new RecruitmentNoticeResponse(
                notice.getId(),
                notice.getRequestId(),
                notice.getHostClientId(),
                notice.getExpoId(),
                venueHallId,
                venueHallLayoutFileId,
                venueZoneIds,
                venueZoneLayoutFileIds,
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
