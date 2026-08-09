package com.expo.admin.controller;

import com.expo.admin.dto.CategoryCreateRequest;
import com.expo.admin.dto.CategoryResponse;
import com.expo.admin.dto.CategoryUpdateRequest;
import com.expo.admin.service.CategoryService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 카테고리 관리 (E-API-014~017). */
@Tag(name = "Admin Category", description = "관리자 카테고리 관리")
@RestController
@RequestMapping("/api/admin/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "관리자 카테고리 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getCategories()));
    }

    @Operation(summary = "박람회 카테고리 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(categoryService.createCategory(request)));
    }

    @Operation(summary = "카테고리명·노출순서·활성상태 수정")
    @PatchMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long categoryId, @Valid @RequestBody CategoryUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.updateCategory(categoryId, request)));
    }

    @Operation(summary = "미사용 카테고리 비활성/삭제")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deactivateCategory(@PathVariable Long categoryId) {
        categoryService.deactivateCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
