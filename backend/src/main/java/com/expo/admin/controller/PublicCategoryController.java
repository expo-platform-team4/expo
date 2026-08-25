package com.expo.admin.controller;

import com.expo.admin.dto.CategoryResponse;
import com.expo.admin.service.CategoryService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 카테고리 공개 조회. 박람회 개최 신청 폼(카테고리 선택)·검색 화면(카테고리 필터)이 쓴다.
 *
 * <p>{@code CategoryController}(어드민 전용, `/api/admin/categories`)와 다르다 — 여기는 비활성 카테고리를 뺀
 * {@link CategoryService#getPublicCategories()} 를 쓴다.
 */
@Tag(name = "Public Category", description = "카테고리 공개 조회")
@RestController
@RequestMapping("/api/categories")
public class PublicCategoryController {

    private final CategoryService categoryService;

    public PublicCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "카테고리 목록 조회 (공개, 활성만)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getPublicCategories()));
    }
}
