package com.expo.venue.controller;

import com.expo.common.response.ApiResponse;
import com.expo.venue.dto.CreateVirtualVenueRequest;
import com.expo.venue.dto.VirtualVenueResponse;
import com.expo.venue.service.VirtualVenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 가상 장소 관리. */
@Tag(name = "Admin Virtual Venue", description = "관리자 가상 장소 관리")
@RestController
@RequestMapping("/api/admin/virtual-venues")
public class VirtualVenueController {

    private final VirtualVenueService virtualVenueService;

    public VirtualVenueController(VirtualVenueService virtualVenueService) {
        this.virtualVenueService = virtualVenueService;
    }

    @Operation(summary = "가상 장소 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VirtualVenueResponse>>> getVirtualVenues() {
        return ResponseEntity.ok(ApiResponse.ok(virtualVenueService.list()));
    }

    @Operation(summary = "가상 장소 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<VirtualVenueResponse>> createVirtualVenue(
            @Valid @RequestBody CreateVirtualVenueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(virtualVenueService.create(request)));
    }
}
