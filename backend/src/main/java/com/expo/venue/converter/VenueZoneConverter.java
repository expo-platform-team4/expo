package com.expo.venue.converter;

import com.expo.venue.dto.VenueZoneResponse;
import com.expo.venue.entity.VenueZone;
import org.springframework.stereotype.Component;

@Component
public class VenueZoneConverter {

    public VenueZoneResponse toResponse(VenueZone zone) {
        return new VenueZoneResponse(
                zone.getId(),
                zone.getHallId(),
                zone.getZoneCode(),
                zone.getName(),
                zone.getMaxBoothCount(),
                zone.getWidth(),
                zone.getDepth(),
                zone.getLayoutFileId(),
                zone.getOperationalStatus(),
                zone.getCreatedAt(),
                zone.getUpdatedAt());
    }
}
