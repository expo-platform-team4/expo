package com.expo.banner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * 배너 노출 위치와 최대 동시 노출 수 (V1: banner_slots)
 *
 * "현재 노출 가능한 메인 배너 최대 5개" (B-API-024) 는 이 슬롯의 max_active_count 값에서
 * 온다. 5는 하드코딩이 아니라 특정 slot(예: MAIN_TOP)의 설정값이다.
 */
@Entity
@Table(name = "banner_slots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BannerSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slot_code", length = 50, nullable = false, unique = true)
    private String slotCode;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "max_active_count", nullable = false)
    private Integer maxActiveCount;

    @Column(name = "width_px")
    private Integer widthPx;

    @Column(name = "height_px")
    private Integer heightPx;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** 이 슬롯에 배너를 하나 더 활성화할 여유가 있는지 (승인 시 정원 체크용) */
    public boolean hasCapacity(long currentActiveCount) {
        return currentActiveCount < maxActiveCount;
    }
}
