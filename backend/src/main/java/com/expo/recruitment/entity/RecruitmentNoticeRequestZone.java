package com.expo.recruitment.entity;

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
 * 모집공고 생성 요청이 고른 구역(홀) 한 건.
 *
 * <p>요청 하나가 같은 전시관 안에서 구역을 여러 개 고를 수 있어 (request_id, venue_zone_id) 복합키를 쓴다. 고른 구역이
 * 요청의 venue_hall_id 소속인지는 DB의 복합 FK({@code fk_notice_request_zones_zone_in_hall})가 강제한다.
 *
 * <p>{@code created_at} 만 있고 {@code updated_at} 이 없는 append-only 라 {@code BaseTimeEntity} 를 상속하지
 * 않는다.
 */
@Getter
@Entity
@Table(name = "recruitment_notice_request_zones")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentNoticeRequestZone {

    @EmbeddedId private Id id;

    @Column(name = "venue_hall_id", nullable = false)
    private Long venueHallId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static RecruitmentNoticeRequestZone create(
            Long requestId, Long venueHallId, Long venueZoneId) {
        RecruitmentNoticeRequestZone zone = new RecruitmentNoticeRequestZone();
        zone.id = new Id(requestId, venueZoneId);
        zone.venueHallId = venueHallId;
        return zone;
    }

    public Long getRequestId() {
        return id.requestId;
    }

    public Long getVenueZoneId() {
        return id.venueZoneId;
    }

    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Id implements Serializable {

        @Column(name = "request_id")
        private Long requestId;

        @Column(name = "venue_zone_id")
        private Long venueZoneId;

        Id(Long requestId, Long venueZoneId) {
            this.requestId = requestId;
            this.venueZoneId = venueZoneId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Id id)) {
                return false;
            }
            return Objects.equals(requestId, id.requestId)
                    && Objects.equals(venueZoneId, id.venueZoneId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(requestId, venueZoneId);
        }
    }
}
