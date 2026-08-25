package com.expo.admin.service;

import com.expo.admin.converter.CategoryConverter;
import com.expo.admin.dto.CategoryCreateRequest;
import com.expo.admin.dto.CategoryResponse;
import com.expo.admin.dto.CategoryUpdateRequest;
import com.expo.admin.entity.Category;
import com.expo.admin.repository.CategoryRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 카테고리 관리 (E-API-014~017). */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryConverter categoryConverter;

    public CategoryService(
            CategoryRepository categoryRepository, CategoryConverter categoryConverter) {
        this.categoryRepository = categoryRepository;
        this.categoryConverter = categoryConverter;
    }

    /** 노출 순서 기준 카테고리 전체 목록을 조회한다 (E-API-014). */
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(categoryConverter::toResponse)
                .toList();
    }

    /**
     * 노출 순서 기준 활성 카테고리 목록을 조회한다. 비로그인·클라이언트 모두 볼 수 있는 공개 조회라
     * 비활성(숨김) 카테고리는 뺀다 — 관리자 목록 조회({@link #getCategories()})와 다른 점이다.
     */
    public List<CategoryResponse> getPublicCategories() {
        return categoryRepository.findAllByActiveTrueOrderBySortOrderAsc().stream()
                .map(categoryConverter::toResponse)
                .toList();
    }

    /** 박람회 카테고리를 등록한다 (E-API-015). */
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        if (categoryRepository.existsBySlug(toSlug(request.name()))) {
            throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY_NAME);
        }
        Category category =
                Category.create(
                        request.parentId(),
                        request.name(),
                        toSlug(request.name()),
                        request.sortOrder() == null ? 0 : request.sortOrder());
        return categoryConverter.toResponse(categoryRepository.save(category));
    }

    /** 카테고리명·노출순서·활성상태를 수정한다 (E-API-016). */
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request) {
        Category category = getCategoryOrThrow(categoryId);
        if (request.name() != null
                && categoryRepository.existsByNameAndIdNot(request.name(), categoryId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY_NAME);
        }
        category.update(request.name(), request.sortOrder(), request.active());
        return categoryConverter.toResponse(category);
    }

    /** 미사용 카테고리를 비활성화한다 (E-API-017). */
    @Transactional
    public void deactivateCategory(Long categoryId) {
        Category category = getCategoryOrThrow(categoryId);
        category.deactivate();
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    /** 카테고리명을 URL-safe 슬러그로 변환한다. 공백을 하이픈으로 바꾸고 소문자화한다. */
    private String toSlug(String name) {
        return name.trim().toLowerCase().replaceAll("\\s+", "-");
    }
}
