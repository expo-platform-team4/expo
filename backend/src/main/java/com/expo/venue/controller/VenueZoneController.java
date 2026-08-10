package com.expo.venue.controller;

import com.expo.common.response.ApiResponse;
import com.expo.venue.dto.CreateVenueZoneRequest;
import com.expo.venue.dto.VenueZoneResponse;
import com.expo.venue.service.VenueZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 구역 관리. */
@Tag(name = "Admin Venue Zone", description = "관리자 구역 관리")
@RestController
@RequestMapping("/api/admin/venue-halls/{hallId}/zones")
public class VenueZoneController {

    private final VenueZoneService venueZoneService;

    public VenueZoneController(VenueZoneService venueZoneService) {
        this.venueZoneService = venueZoneService;
    }

    @Operation(summary = "홀 내 구역 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<VenueZoneResponse>> createVenueZone(
            @PathVariable Long hallId, @Valid @RequestBody CreateVenueZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(venueZoneService.create(hallId, request)));
    }
}
