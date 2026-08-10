package com.expo.venue.converter;

import com.expo.venue.dto.VenueReservationResponse;
import com.expo.venue.entity.VenueReservation;
import org.springframework.stereotype.Component;

@Component
public class VenueReservationConverter {

    public VenueReservationResponse toResponse(VenueReservation reservation) {
        return new VenueReservationResponse(
                reservation.getId(),
                reservation.getReservationSourceType(),
                reservation.getNoticeRequestId(),
                reservation.getVirtualVenueId(),
                reservation.getVenueHallId(),
                reservation.getVenueZoneId(),
                reservation.getUseStartAt(),
                reservation.getUseEndAt(),
                reservation.getStatus(),
                reservation.getConfirmedByAdminId(),
                reservation.getConfirmedAt(),
                reservation.getReleasedAt());
    }
}
