package com.expo.expo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 박람회 ↔ 카테고리 N:M 연결 (V1: expo_categories).
 *
 * <p>승인 시점에 {@code ExpoOpeningRequestCategory}(신청 단계의 임시 연결)를 그대로 복사해서 만든다 —
 * {@code ExpoOpeningRequestService.approve()} 참고.
 */
@Getter
@Entity
@Table(name = "expo_categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpoCategory {

    @EmbeddedId private Id id;

    public static ExpoCategory create(Long expoId, Long categoryId) {
        ExpoCategory entity = new ExpoCategory();
        entity.id = new Id(expoId, categoryId);
        return entity;
    }

    public Long getExpoId() {
        return id.expoId;
    }

    public Long getCategoryId() {
        return id.categoryId;
    }

    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Id implements Serializable {

        @Column(name = "expo_id")
        private Long expoId;

        @Column(name = "category_id")
        private Long categoryId;

        Id(Long expoId, Long categoryId) {
            this.expoId = expoId;
            this.categoryId = categoryId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Id id)) {
                return false;
            }
            return Objects.equals(expoId, id.expoId) && Objects.equals(categoryId, id.categoryId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(expoId, categoryId);
        }
    }
}
