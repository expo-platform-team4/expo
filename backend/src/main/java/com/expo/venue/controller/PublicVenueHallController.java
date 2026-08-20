package com.expo.venue.controller;

import com.expo.common.response.ApiResponse;
import com.expo.venue.dto.VenueHallResponse;
import com.expo.venue.service.VenueHallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 홀 열람. */
@Tag(name = "Venue Hall", description = "공개 홀 열람")
@RestController
@RequestMapping("/api/virtual-venues/{venueId}/halls")
public class PublicVenueHallController {

    private final VenueHallService venueHallService;

    public PublicVenueHallController(VenueHallService venueHallService) {
        this.venueHallService = venueHallService;
    }

    @Operation(summary = "장소 내 홀 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VenueHallResponse>>> list(@PathVariable Long venueId) {
        return ResponseEntity.ok(ApiResponse.ok(venueHallService.list(venueId)));
    }
}
