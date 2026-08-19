package com.expo.venue.converter;

import com.expo.venue.dto.VenueReservationHistoryResponse;
import com.expo.venue.dto.VenueReservationResponse;
import com.expo.venue.entity.VenueReservation;
import com.expo.venue.entity.VenueReservationHistory;
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

    public VenueReservationHistoryResponse toHistoryResponse(VenueReservationHistory history) {
        return new VenueReservationHistoryResponse(
                history.getId(),
                history.getActionType(),
                history.getReason(),
                history.getProcessedByAdminId(),
                history.getCreatedAt());
    }
}
