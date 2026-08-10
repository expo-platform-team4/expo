package com.expo.admin.entity;

import com.expo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** CATEGORIES 테이블 — 검색·필터용 카테고리 마스터 (E-API-014~017). */
@Getter
@Entity
@Table(name = "categories")
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false, length = 100, unique = true)
    private String slug;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    protected Category() {}

    public static Category create(Long parentId, String name, String slug, int sortOrder) {
        Category category = new Category();
        category.parentId = parentId;
        category.name = name;
        category.slug = slug;
        category.sortOrder = sortOrder;
        category.active = true;
        return category;
    }

    /** 카테고리명·노출순서·활성상태를 수정한다. null인 필드는 변경하지 않는다. */
    public void update(String name, Integer sortOrder, Boolean active) {
        if (name != null) {
            this.name = name;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        if (active != null) {
            this.active = active;
        }
    }

    public void deactivate() {
        this.active = false;
    }
}
