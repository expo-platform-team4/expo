package com.expo.venue.controller;

import com.expo.common.response.ApiResponse;
import com.expo.venue.dto.CreateVenueHallRequest;
import com.expo.venue.dto.VenueHallResponse;
import com.expo.venue.service.VenueHallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 홀 등록·조회. */
@Tag(name = "Admin Venue Hall", description = "관리자 홀 등록·조회")
@RestController
@RequestMapping("/api/admin/virtual-venues/{venueId}/halls")
public class VenueHallController {

    private final VenueHallService venueHallService;

    public VenueHallController(VenueHallService venueHallService) {
        this.venueHallService = venueHallService;
    }

    @Operation(summary = "장소 내 홀 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<VenueHallResponse>> createVenueHall(
            @PathVariable Long venueId, @Valid @RequestBody CreateVenueHallRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(venueHallService.create(venueId, request)));
    }

    @Operation(summary = "장소 내 홀 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VenueHallResponse>>> list(@PathVariable Long venueId) {
        return ResponseEntity.ok(ApiResponse.ok(venueHallService.list(venueId)));
    }

    @Operation(summary = "장소 내 홀 상세 조회")
    @GetMapping("/{hallId}")
    public ResponseEntity<ApiResponse<VenueHallResponse>> get(
            @PathVariable Long venueId, @PathVariable Long hallId) {
        return ResponseEntity.ok(ApiResponse.ok(venueHallService.get(venueId, hallId)));
    }
}
