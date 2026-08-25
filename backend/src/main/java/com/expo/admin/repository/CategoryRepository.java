package com.expo.admin.repository;

import com.expo.admin.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@link Category} 리포지토리. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** 노출 순서 기준으로 카테고리 전체 목록을 조회한다. */
    List<Category> findAllByOrderBySortOrderAsc();

    /** 노출 순서 기준으로 활성 카테고리만 조회한다. 공개 API(박람회 개최 신청 폼 등)가 쓴다. */
    List<Category> findAllByActiveTrueOrderBySortOrderAsc();

    boolean existsBySlug(String slug);

    /** 자기 자신을 제외하고 같은 이름의 카테고리가 있는지 확인한다. */
    boolean existsByNameAndIdNot(String name, Long id);
}
