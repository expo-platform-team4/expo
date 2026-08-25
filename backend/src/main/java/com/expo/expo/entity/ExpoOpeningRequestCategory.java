package com.expo.expo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 박람회 개최 신청이 고른 카테고리 한 건.
 *
 * <p>신청 하나가 카테고리를 여러 개 고를 수 있어 (opening_request_id, category_id) 복합키를 쓴다.
 * {@code RecruitmentNoticeRequestZone} 과 같은 패턴 — 승인 전까지의 임시 연결이고, 승인되면
 * {@code ExpoOpeningRequestService.approve()} 가 이 목록을 그대로 {@code expo_categories} 로 복사한다.
 *
 * <p>{@code created_at} 만 있고 {@code updated_at} 이 없는 append-only 라 {@code BaseTimeEntity} 를 상속하지
 * 않는다.
 */
@Getter
@Entity
@Table(name = "expo_opening_request_categories")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpoOpeningRequestCategory {

    @EmbeddedId private Id id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static ExpoOpeningRequestCategory create(Long openingRequestId, Long categoryId) {
        ExpoOpeningRequestCategory entity = new ExpoOpeningRequestCategory();
        entity.id = new Id(openingRequestId, categoryId);
        return entity;
    }

    public Long getOpeningRequestId() {
        return id.openingRequestId;
    }

    public Long getCategoryId() {
        return id.categoryId;
    }

    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Id implements Serializable {

        @Column(name = "opening_request_id")
        private Long openingRequestId;

        @Column(name = "category_id")
        private Long categoryId;

        Id(Long openingRequestId, Long categoryId) {
            this.openingRequestId = openingRequestId;
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
            return Objects.equals(openingRequestId, id.openingRequestId)
                    && Objects.equals(categoryId, id.categoryId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(openingRequestId, categoryId);
        }
    }
}
