package com.expo.venue.controller;

import com.expo.common.response.ApiResponse;
import com.expo.venue.dto.VenueAvailabilityResponse;
import com.expo.venue.service.VenueReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 장소·홀·구역 가용성 조회. */
@Tag(name = "Virtual Venue Availability", description = "장소·홀·구역 예약 가능 여부 조회")
@RestController
@RequestMapping("/api/virtual-venues/{venueId}/availability")
public class VenueAvailabilityController {

    private final VenueReservationService venueReservationService;

    public VenueAvailabilityController(VenueReservationService venueReservationService) {
        this.venueReservationService = venueReservationService;
    }

    @Operation(summary = "장소·홀·구역 기간 중복 및 예약 가능 여부 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<VenueAvailabilityResponse>> checkAvailability(
            @PathVariable Long venueId,
            @RequestParam(required = false) Long hallId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        venueReservationService.checkAvailability(
                                venueId, hallId, zoneId, startAt, endAt)));
    }
}
