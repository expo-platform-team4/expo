package com.expo.venue.converter;

import com.expo.venue.dto.VenueHallResponse;
import com.expo.venue.entity.VenueHall;
import org.springframework.stereotype.Component;

@Component
public class VenueHallConverter {

    public VenueHallResponse toResponse(VenueHall hall) {
        return new VenueHallResponse(
                hall.getId(),
                hall.getVenueId(),
                hall.getHallCode(),
                hall.getName(),
                hall.getWidth(),
                hall.getDepth(),
                hall.getLayoutFileId(),
                hall.getOperationalStatus(),
                hall.getCreatedAt(),
                hall.getUpdatedAt());
    }
}
