package com.expo.expo.entity;

import com.expo.expo.entity.ExpoEnums.EventStatus;
import com.expo.expo.entity.ExpoEnums.ReviewStatus;
import com.expo.expo.entity.ExpoEnums.VisibilityStatus;
import com.expo.expo.exception.ExpoStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 박람회 원본 (V1: expos)
 *
 * 개최 신청(expo_opening_requests)이 승인되는 시점에 생성되며,
 * 티켓 판매·검색 노출·체크인의 기준이 된다.
 * 승인과 동시에 visibility_status=PUBLIC 으로 자동 공개된다 (희-EXPO-09 → 희-SRCH-12).
 */
@Entity
@Table(name = "expos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 주최 클라이언트 — FK(client_profiles.user_id) */
    @Column(name = "host_client_id", nullable = false)
    private Long hostClientId;

    /** 원천 개최 신청 — FK(expo_opening_requests.id), UNIQUE */
    @Column(name = "opening_request_id", unique = true)
    private Long openingRequestId;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    /** 검색·필터용 지역 코드 (희-SRCH-03) — 확정 장소의 virtual_venues.region_code 에서 복사 */
    @Column(name = "region_code", length = 30, nullable = false)
    private String regionCode;

    @Column(name = "event_start_at", nullable = false)
    private OffsetDateTime eventStartAt;

    @Column(name = "event_end_at", nullable = false)
    private OffsetDateTime eventEndAt;

    /** 판매 시작·종료 (희-EXPO-10 판매 상태 자동 계산의 기준) */
    @Column(name = "sales_start_at", nullable = false)
    private OffsetDateTime salesStartAt;

    @Column(name = "sales_end_at", nullable = false)
    private OffsetDateTime salesEndAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 20, nullable = false)
    private ReviewStatus reviewStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_status", length = 20, nullable = false)
    private VisibilityStatus visibilityStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", length = 20, nullable = false)
    private EventStatus eventStatus;

    @Column(name = "approved_by_admin_id")
    private Long approvedByAdminId;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "canceled_at")
    private OffsetDateTime canceledAt;

    /** 낙관적 락 (V1: version BIGINT DEFAULT 0) */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 승인된 개최 신청으로부터 공개 박람회를 생성한다 (희-EXPO-09 승인 시 자동 공개).
     *
     * @param regionCode 확정(또는 희망) 장소의 지역 코드
     */
    public static Expo publishFrom(ExpoOpeningRequest request, String regionCode, Long adminId) {
        Expo expo = new Expo();
        expo.hostClientId = request.getHostClientId();
        expo.openingRequestId = request.getId();
        expo.title = request.getTitle();
        expo.description = request.getDescription();
        expo.regionCode = regionCode;
        expo.eventStartAt = request.getEventStartAt();
        expo.eventEndAt = request.getEventEndAt();
        expo.salesStartAt = request.getSalesStartAt();
        expo.salesEndAt = request.getSalesEndAt();
        expo.reviewStatus = ReviewStatus.APPROVED;
        expo.visibilityStatus = VisibilityStatus.PUBLIC; // 승인 = 자동 공개 → 목록 자동 반영 (희-SRCH-12)
        expo.eventStatus = EventStatus.SCHEDULED;
        expo.approvedByAdminId = adminId;
        expo.approvedAt = OffsetDateTime.now();
        return expo;
    }

    /** 공개 목록 노출 대상인지 (희-SRCH-12) */
    public boolean isPubliclyVisible() {
        return visibilityStatus == VisibilityStatus.PUBLIC
                && reviewStatus == ReviewStatus.APPROVED
                && eventStatus != EventStatus.CANCELED;
    }

    /** 행사 취소 처리 — 취소 요청(expo_cancellation_requests) 승인 시 호출 */
    public void cancelEvent() {
        if (eventStatus == EventStatus.CANCELED) {
            throw new ExpoStateException("이미 취소된 박람회입니다.");
        }
        this.eventStatus = EventStatus.CANCELED;
        this.canceledAt = OffsetDateTime.now();
        this.visibilityStatus = VisibilityStatus.ARCHIVED;
    }

    /** 행사 종료 처리 — 스케줄러가 event_end_at 경과 시 호출 */
    public void closeEvent() {
        if (eventStatus == EventStatus.SCHEDULED || eventStatus == EventStatus.ONGOING) {
            this.eventStatus = EventStatus.ENDED;
        }
    }

    public boolean isOwnedBy(Long clientId) {
        return this.hostClientId != null && this.hostClientId.equals(clientId);
    }
}
