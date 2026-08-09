package com.expo.booth.controller;

import com.expo.booth.dto.BoothTemplateResponse;
import com.expo.booth.dto.CreateBoothTemplateRequest;
import com.expo.booth.service.BoothTemplateService;
import com.expo.common.response.ApiResponse;
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

/** 관리자 부스 템플릿 관리. */
@Tag(name = "Admin Booth Template", description = "관리자 부스 템플릿 관리")
@RestController
@RequestMapping("/api/admin/booth-templates")
public class BoothTemplateController {

    private final BoothTemplateService boothTemplateService;

    public BoothTemplateController(BoothTemplateService boothTemplateService) {
        this.boothTemplateService = boothTemplateService;
    }

    @Operation(summary = "부스 템플릿 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BoothTemplateResponse>>> getBoothTemplates() {
        return ResponseEntity.ok(ApiResponse.ok(boothTemplateService.list()));
    }

    @Operation(summary = "부스 템플릿 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<BoothTemplateResponse>> createBoothTemplate(
            @Valid @RequestBody CreateBoothTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(boothTemplateService.create(request)));
    }
}
