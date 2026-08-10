package com.expo.venue.converter;

import com.expo.venue.dto.VirtualVenueResponse;
import com.expo.venue.entity.VirtualVenue;
import org.springframework.stereotype.Component;

@Component
public class VirtualVenueConverter {

    public VirtualVenueResponse toResponse(VirtualVenue venue) {
        return new VirtualVenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getAddress(),
                venue.getRegionCode(),
                venue.getDescription(),
                venue.getMapFileId(),
                venue.getOperationalStatus(),
                venue.getCreatedAt(),
                venue.getUpdatedAt());
    }
}
