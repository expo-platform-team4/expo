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
import java.time.Instant;
import java.time.OffsetDateTime;

import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 박람회.
 *
 * <p>원래 {@code id}·{@code hostClientId} 만 매핑한 스텁이었다. 개최 신청 승인이 이 행을 만들어야 해서(이슈 #116)
 * NOT NULL 컬럼과 승인 흔적을 채울 만큼만 넓혔다. 여전히 전체 컬럼을 다 매핑하지는 않는다 — {@code ddl-auto: validate}
 * 는 매핑한 컬럼이 실제로 있는지만 보고 빠짐은 문제 삼지 않는다.
 *
 * <p>상태 세 가지는 {@code expos} 의 CHECK 제약과 값이 같아야 해서 문자열로 다룬다. 전용 enum 은 이 값을 쓰는 쪽이
 * 늘어날 때 만든다 — 지금 만들면 쓰는 곳이 여기 하나뿐이다.
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

    /** 이 박람회를 만들어 낸 개최 신청. UNIQUE 라 신청 1건당 박람회 1개다. */
    @Column(name = "opening_request_id")
    private Long openingRequestId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "region_code", nullable = false, length = 30)
    private String regionCode;

    @Column(name = "event_start_at", nullable = false)
    private Instant eventStartAt;

    @Column(name = "event_end_at", nullable = false)
    private Instant eventEndAt;

    @Column(name = "sales_start_at", nullable = false)
    private Instant salesStartAt;

    @Column(name = "sales_end_at", nullable = false)
    private Instant salesEndAt;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus;

    @Column(name = "visibility_status", nullable = false, length = 20)
    private String visibilityStatus;

    @Column(name = "event_status", nullable = false, length = 20)
    private String eventStatus;

    @Column(name = "approved_by_admin_id")
    private Long approvedByAdminId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    protected Expo() {}

    /**
     * 승인된 개최 신청으로부터 박람회를 만든다.
     *
     * <p>상태 셋은 고정이다 — 승인 직후이므로 심사는 APPROVED, 아직 행사 전이라 SCHEDULED, 그리고 승인됐으면 공개하는
     * 것이 자연스러워 PUBLIC 으로 연다. 비공개로 두고 주최사가 따로 여는 흐름은 화면이 없어 만들지 않았다.
     *
     * @param regionCode 희망 장소({@code virtual_venues.region_code})에서 파생한 값. 신청서에는 지역 컬럼이 없다.
     */
    public static Expo createFromOpeningRequest(
            ExpoOpeningRequest request, String regionCode, Long approvedByAdminId, Instant now) {
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
        expo.reviewStatus = "APPROVED";
        expo.visibilityStatus = "PUBLIC";
        expo.eventStatus = "SCHEDULED";
        expo.approvedByAdminId = approvedByAdminId;
        expo.approvedAt = now;
        return expo;
    }

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
