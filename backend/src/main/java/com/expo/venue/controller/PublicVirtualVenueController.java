package com.expo.venue.controller;

import com.expo.common.response.ApiResponse;
import com.expo.venue.dto.VirtualVenueResponse;
import com.expo.venue.service.VirtualVenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 가상 장소 열람. */
@Tag(name = "Virtual Venue", description = "공개 가상 장소 열람")
@RestController
@RequestMapping("/api/virtual-venues")
public class PublicVirtualVenueController {

    private final VirtualVenueService virtualVenueService;

    public PublicVirtualVenueController(VirtualVenueService virtualVenueService) {
        this.virtualVenueService = virtualVenueService;
    }

    @Operation(summary = "가상 장소 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VirtualVenueResponse>>> getVirtualVenues() {
        return ResponseEntity.ok(ApiResponse.ok(virtualVenueService.list()));
    }
}
