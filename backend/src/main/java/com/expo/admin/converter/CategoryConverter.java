package com.expo.admin.converter;

import com.expo.admin.dto.CategoryResponse;
import com.expo.admin.entity.Category;
import org.springframework.stereotype.Component;

/** {@link Category} ↔ DTO 변환. */
@Component
public class CategoryConverter {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getParentId(),
                category.getName(),
                category.getSlug(),
                category.getSortOrder(),
                category.isActive());
    }
}
