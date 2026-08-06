package com.expo.expo.domain;

import com.example.expo.exception.ExpoStateException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 박람회(EXPO) 엔티티 — EXPO_TICKET_테이블정의서_v2.xlsx / EXPO 시트 기반
 *
 * 클라이언트가 신청한 박람회의 기본정보와 심사·공개 상태를 관리하는 핵심 테이블.
 * is_deleted = TRUE 인 레코드는 서비스 화면에서 조회 제외(논리 삭제 정책, 비고 6).
 */
@Entity
@Table(
    name = "expo",
    indexes = {
        @Index(name = "idx_expo_status", columnList = "status"),
        @Index(name = "idx_expo_category", columnList = "category_id"),
        @Index(name = "idx_expo_client", columnList = "client_id"),
        @Index(name = "idx_expo_period", columnList = "start_date, end_date")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expo {

    /** 박람회 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expo_id")
    private Long expoId;

    /** 박람회를 등록한 클라이언트(주최자) — FK(client.client_id) */
    @Column(name = "client_id", nullable = false)
    private Long clientId;

    /** 박람회 분류 카테고리 — FK(category.category_id) */
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    /** 신청 시 입력한 희망 장소 텍스트 (예: 코엑스 A홀) — 희-EXPO-17 */
    @Column(name = "desired_venue", length = 200)
    private String desiredVenue;

    /** 관리자 승인 시점에 확정 배정되는 실제 장소 — FK(venue.venue_id) */
    @Column(name = "venue_id")
    private Long venueId;

    /** 박람회 제목 (DRAFT 단계 NULL 허용, 심사요청 시 앱 레벨 필수 검증 — 비고 3) */
    @Column(name = "title", length = 200)
    private String title;

    /** 행사 목적, 주요 프로그램 등 상세 내용 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 박람회 개최 시작일 */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** 박람회 개최 종료일 */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** 목록/상세 노출용 대표 이미지 경로 (권장 1920x1080) — 희-EXPO-15 */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /** 검색·필터용 지역명 (드롭다운 선택값) — 희-SRCH-03 */
    @Column(name = "region", length = 100)
    private String region;

    /** 심사/공개 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ExpoStatus status = ExpoStatus.DRAFT;

    /** 취소 요청 사유 */
    @Column(name = "cancel_reason", length = 1000)
    private String cancelReason;

    /** 취소 요청 시각 */
    @Column(name = "cancel_requested_at")
    private LocalDateTime cancelRequestedAt;

    /** 취소 승인·환불 완료 시각 */
    @Column(name = "cancel_approved_at")
    private LocalDateTime cancelApprovedAt;

    /** 논리 삭제 플래그 (비고 6) */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    /** 레코드 생성 시각 */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 레코드 최종 수정 시각 */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Expo(Long clientId, Long categoryId, String desiredVenue, String title,
                 String description, LocalDate startDate, LocalDate endDate,
                 String thumbnailUrl, String region) {
        this.clientId = clientId;
        this.categoryId = categoryId;
        this.desiredVenue = desiredVenue;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.thumbnailUrl = thumbnailUrl;
        this.region = region;
        this.status = ExpoStatus.DRAFT;
        this.isDeleted = Boolean.FALSE;
    }

    /* ==================== 비즈니스 로직 ==================== */

    /**
     * 임시저장 상태에서 내용 갱신 / 승인 전 클라이언트 직접 수정
     * (희-EXPO-01, 희-EXPO-05, 희-EXPO-06)
     *
     * @throws ExpoStateException 승인(PUBLISHED) 이후 수정 시도 시
     */
    public void updateByClient(Long categoryId, String desiredVenue, String title,
                               String description, LocalDate startDate, LocalDate endDate,
                               String thumbnailUrl, String region) {
        if (!status.isEditableByClient()) {
            // 희-EXPO-06 승인 후 클라이언트 직접 수정 제한
            throw new ExpoStateException(
                "승인된 박람회는 직접 수정할 수 없습니다. 관리자에게 수정을 요청해 주세요. (현재 상태: " + status + ")");
        }
        if (categoryId != null) this.categoryId = categoryId;
        if (desiredVenue != null) this.desiredVenue = desiredVenue;
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (startDate != null) this.startDate = startDate;
        if (endDate != null) this.endDate = endDate;
        if (thumbnailUrl != null) this.thumbnailUrl = thumbnailUrl;
        if (region != null) this.region = region;
        validatePeriod();
    }

    /** 관리자 심사 시작 */
    public void startReview() {
        if (status != ExpoStatus.SUBMITTED) {
            throw new ExpoStateException("심사요청 상태가 아닙니다. (현재 상태: " + status + ")");
        }
        this.status = ExpoStatus.UNDER_REVIEW;
    }

    /** 취소 요청 */
    public void requestCancellation(String reason) {
        if (status != ExpoStatus.PUBLISHED) {
            throw new ExpoStateException("공개 상태의 박람회만 취소 요청할 수 있습니다. (현재 상태: " + status + ")");
        }
        this.status = ExpoStatus.CANCELLATION_REQUESTED;
        this.cancelReason = reason;
        this.cancelRequestedAt = LocalDateTime.now();
    }

    /** 취소 승인 (환불 완료 처리 포함) */
    public void approveCancellation() {
        if (status != ExpoStatus.CANCELLATION_REQUESTED) {
            throw new ExpoStateException("취소요청 상태가 아닙니다. (현재 상태: " + status + ")");
        }
        this.status = ExpoStatus.CANCELLED;
        this.cancelApprovedAt = LocalDateTime.now();
    }

    /** 대표 이미지 URL 변경 (희-EXPO-15) — 수정 가능 상태 검증 포함 */
    public void changeThumbnail(String thumbnailUrl) {
        if (!status.isEditableByClient()) {
            throw new ExpoStateException("승인된 박람회는 직접 수정할 수 없습니다.");
        }
        this.thumbnailUrl = thumbnailUrl;
    }

    /** 논리 삭제 (비고 6) */
    public void softDelete() {
        this.isDeleted = Boolean.TRUE;
    }

    /** 소유자 검증 */
    public boolean isOwnedBy(Long clientId) {
        return this.clientId != null && this.clientId.equals(clientId);
    }

    /* ==================== 내부 검증 ==================== */

    private void validateRequiredFieldsForSubmit() {
        if (!StringUtils.hasText(title)) throw new ExpoStateException("박람회명은 필수입니다.");
        if (!StringUtils.hasText(description)) throw new ExpoStateException("상세 소개는 필수입니다.");
        if (startDate == null) throw new ExpoStateException("행사 시작일은 필수입니다.");
        if (endDate == null) throw new ExpoStateException("행사 종료일은 필수입니다.");
        if (!StringUtils.hasText(thumbnailUrl)) throw new ExpoStateException("대표 이미지는 필수입니다.");
        if (!StringUtils.hasText(desiredVenue)) throw new ExpoStateException("희망 장소는 필수입니다.");
        validatePeriod();
    }

    private void validatePeriod() {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ExpoStateException("행사 종료일은 시작일 이후여야 합니다.");
        }
    }
}
