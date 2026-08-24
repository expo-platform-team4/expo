package com.expo.banner.entity;

import com.expo.banner.exception.BannerStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 승인과 동시에 자동 생성되는 실제 노출 배너 (V1: banners)
 *
 * {@link BannerApplication} 이 승인되면 이 레코드가 생성되고, 시작일시가 이미 지났으면
 * 즉시 ACTIVE 로, 아직이면 SCHEDULED 로 만들어진다 (B-API-022 즉시/예약 자동 활성화).
 * SCHEDULED → ACTIVE, ACTIVE → ENDED 전이는 스케줄러가 start_at / end_at 도달 시 호출한다.
 *
 * B-API-024(현재 노출 가능한 메인 배너 최대 5개)는 이 테이블에서
 * display_status = ACTIVE && banner_slot_id = 대상 슬롯 조건으로 조회하며,
 * 최대 개수는 BannerSlot.maxActiveCount 로 관리한다(하드코딩 아님).
 */
@Entity
@Table(name = "banners")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 원천 배너 신청 — FK(banner_applications.id), UNIQUE */
    @Column(name = "banner_application_id", nullable = false, unique = true)
    private Long bannerApplicationId;

    /** 노출 위치 — FK(banner_slots.id) */
    @Column(name = "banner_slot_id", nullable = false)
    private Long bannerSlotId;

    /** 홍보 대상 박람회 — FK(expos.id) */
    @Column(name = "expo_id", nullable = false)
    private Long expoId;

    @Column(name = "image_file_id", nullable = false)
    private Long imageFileId;

    @Column(name = "headline", length = 150)
    private String headline;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_status", length = 20, nullable = false)
    private DisplayStatus displayStatus;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 배너 신청 승인 시 호출 (B-API-022). now 가 희망 시작일시 이후면 즉시 ACTIVE,
     * 이전이면 SCHEDULED 로 생성해 스케줄러가 나중에 activate() 하도록 남겨둔다.
     */
    public static Banner activateFrom(
            BannerApplication application, BannerSlot slot, int sortOrder, OffsetDateTime now) {
        Banner banner = new Banner();
        banner.bannerApplicationId = application.getId();
        banner.bannerSlotId = slot.getId();
        banner.expoId = application.getExpoId();
        banner.imageFileId = application.getImageFileId();
        banner.headline = application.getHeadline();
        banner.startAt = application.getRequestedStartAt();
        banner.endAt = application.getRequestedEndAt();
        banner.sortOrder = sortOrder;
        if (!now.isBefore(banner.startAt)) {
            banner.displayStatus = DisplayStatus.ACTIVE;
            banner.activatedAt = now;
        } else {
            banner.displayStatus = DisplayStatus.SCHEDULED;
        }
        return banner;
    }

    /** 스케줄러가 start_at 도달 시 호출 */
    public void activate(OffsetDateTime now) {
        if (displayStatus != DisplayStatus.SCHEDULED) {
            throw new BannerStateException(
                    "예약 상태가 아니어서 활성화할 수 없습니다. (현재 상태: " + displayStatus + ")");
        }
        this.displayStatus = DisplayStatus.ACTIVE;
        this.activatedAt = now;
    }

    /** 스케줄러가 end_at 도달 시 호출 */
    public void end(OffsetDateTime now) {
        if (displayStatus == DisplayStatus.CANCELED || displayStatus == DisplayStatus.ENDED) {
            return;
        }
        this.displayStatus = DisplayStatus.ENDED;
        this.endedAt = now;
    }

    /** 원천 신청이 취소되거나 관리자가 강제 중단할 때 */
    public void cancel() {
        this.displayStatus = DisplayStatus.CANCELED;
    }

    public boolean isCurrentlyActive() {
        return displayStatus == DisplayStatus.ACTIVE;
    }

    /** banners.display_status */
    public enum DisplayStatus {
        SCHEDULED,
        ACTIVE,
        ENDED,
        CANCELED
    }
}
