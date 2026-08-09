package com.expo.admin.repository;

import com.expo.admin.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@link Category} 리포지토리. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** 노출 순서 기준으로 카테고리 전체 목록을 조회한다. */
    List<Category> findAllByOrderBySortOrderAsc();

    boolean existsBySlug(String slug);
}
