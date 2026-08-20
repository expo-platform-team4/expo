package com.expo.venue.controller;

import com.expo.common.response.ApiResponse;
import com.expo.venue.dto.VenueZoneResponse;
import com.expo.venue.service.VenueZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 구역 열람. */
@Tag(name = "Venue Zone", description = "공개 구역 열람")
@RestController
@RequestMapping("/api/venue-halls/{hallId}/zones")
public class PublicVenueZoneController {

    private final VenueZoneService venueZoneService;

    public PublicVenueZoneController(VenueZoneService venueZoneService) {
        this.venueZoneService = venueZoneService;
    }

    @Operation(summary = "홀 내 구역 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VenueZoneResponse>>> list(@PathVariable Long hallId) {
        return ResponseEntity.ok(ApiResponse.ok(venueZoneService.list(hallId)));
    }
}
