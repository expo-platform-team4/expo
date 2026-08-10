package com.expo.venue.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.venue.dto.CreateVenueReservationRequest;
import com.expo.venue.dto.VenueReservationResponse;
import com.expo.venue.service.VenueReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 장소 예약 관리. */
@Tag(name = "Admin Venue Reservation", description = "관리자 장소 예약 관리")
@RestController
@RequestMapping("/api/admin/venue-reservations")
public class VenueReservationController {

    private final VenueReservationService venueReservationService;

    public VenueReservationController(VenueReservationService venueReservationService) {
        this.venueReservationService = venueReservationService;
    }

    @Operation(summary = "확정 장소 예약 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<VenueReservationResponse>> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateVenueReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                venueReservationService.create(principal.getMemberId(), request)));
    }

    @Operation(summary = "박람회 취소 시 장소 예약 해제")
    @PatchMapping("/{reservationId}/release")
    public ResponseEntity<ApiResponse<VenueReservationResponse>> release(
            @PathVariable Long reservationId) {
        return ResponseEntity.ok(ApiResponse.ok(venueReservationService.release(reservationId)));
    }
}
