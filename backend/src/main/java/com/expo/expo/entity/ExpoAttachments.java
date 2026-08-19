package com.expo.expo.entity;

import com.expo.expo.entity.ExpoEnums.ExpoFilePurpose;
import com.expo.expo.entity.ExpoEnums.ExpoImageType;
import com.expo.expo.entity.ExpoEnums.ExternalLinkType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * 박람회 부속 엔티티 모음 (V1: expo_images / expo_files / external_links / expo_change_requests)
 * 파일 바이너리는 공통 file_metadata 를 통해 관리되고, 여기서는 file_id 만 연결한다.
 */
public final class ExpoAttachments {

    private ExpoAttachments() {}

    /** 썸네일·상세 이미지 (희-EXPO-15 대표 이미지 = image_type THUMBNAIL) */
    @Entity
    @Table(name = "expo_images")
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ExpoImage {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "expo_id", nullable = false)
        private Long expoId;

        /** FK(file_metadata.id) — UNIQUE(expo_id, file_id) */
        @Column(name = "file_id", nullable = false)
        private Long fileId;

        @Enumerated(EnumType.STRING)
        @Column(name = "image_type", length = 20, nullable = false)
        private ExpoImageType imageType;

        @Column(name = "alt_text", length = 255)
        private String altText;

        @Column(name = "sort_order", nullable = false)
        private Integer sortOrder = 0;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private OffsetDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private OffsetDateTime updatedAt;

        @Builder
        private ExpoImage(
                Long expoId,
                Long fileId,
                ExpoImageType imageType,
                String altText,
                Integer sortOrder) {
            this.expoId = expoId;
            this.fileId = fileId;
            this.imageType = imageType;
            this.altText = altText;
            this.sortOrder = sortOrder == null ? 0 : sortOrder;
        }
    }

    /** 소개 PDF·카탈로그·리플렛·홍보영상 (희-EXPO-14/15) */
    @Entity
    @Table(name = "expo_files")
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ExpoFile {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "expo_id", nullable = false)
        private Long expoId;

        @Column(name = "file_id", nullable = false)
        private Long fileId;

        @Enumerated(EnumType.STRING)
        @Column(name = "file_purpose", length = 30, nullable = false)
        private ExpoFilePurpose filePurpose;

        @Column(name = "title", length = 150)
        private String title;

        @Column(name = "sort_order", nullable = false)
        private Integer sortOrder = 0;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private OffsetDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private OffsetDateTime updatedAt;

        @Builder
        private ExpoFile(
                Long expoId,
                Long fileId,
                ExpoFilePurpose filePurpose,
                String title,
                Integer sortOrder) {
            this.expoId = expoId;
            this.fileId = fileId;
            this.filePurpose = filePurpose;
            this.title = title;
            this.sortOrder = sortOrder == null ? 0 : sortOrder;
        }
    }

    /**
     * 외부 링크 (희-EXPO-13).
     * DB CHECK: expo_id / booth_content_id 중 정확히 하나만 채워야 한다.
     * 이 엔티티는 박람회 소속 링크 전용이므로 expo_id 만 사용한다.
     */
    @Entity
    @Table(name = "external_links")
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ExternalLink {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "expo_id")
        private Long expoId;

        @Column(name = "booth_content_id")
        private Long boothContentId;

        @Enumerated(EnumType.STRING)
        @Column(name = "link_type", length = 30, nullable = false)
        private ExternalLinkType linkType;

        @Column(name = "label", length = 100)
        private String label;

        @Column(name = "url", length = 1000, nullable = false)
        private String url;

        @Column(name = "sort_order", nullable = false)
        private Integer sortOrder = 0;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private OffsetDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private OffsetDateTime updatedAt;

        @Builder
        private ExternalLink(
                Long expoId,
                ExternalLinkType linkType,
                String label,
                String url,
                Integer sortOrder) {
            this.expoId = expoId;
            this.boothContentId = null; // 박람회 소속 링크 전용 (owner XOR CHECK)
            this.linkType = linkType;
            this.label = label;
            this.url = url;
            this.sortOrder = sortOrder == null ? 0 : sortOrder;
        }
    }

    /**
     * 승인 후 수정 요청 (희-EXPO-06 대응 경로, V1: expo_change_requests)
     * 승인된 박람회는 직접 수정 대신 이 요청을 통해 관리자가 반영한다.
     */
    @Entity
    @Table(name = "expo_change_requests")
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ExpoChangeRequest {

        public enum Status {
            SUBMITTED,
            UNDER_REVIEW,
            APPLIED,
            REJECTED,
            CANCELED
        }

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "expo_id", nullable = false)
        private Long expoId;

        @Column(name = "requester_client_id", nullable = false)
        private Long requesterClientId;

        @Column(name = "change_reason", columnDefinition = "TEXT", nullable = false)
        private String changeReason;

        /** 변경 희망 필드·값 (JSONB) — 예: {"title": "...", "salesEndAt": "..."} */
        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "requested_changes", columnDefinition = "JSONB", nullable = false)
        private Map<String, Object> requestedChanges;

        @Enumerated(EnumType.STRING)
        @Column(name = "status", length = 20, nullable = false)
        private Status status = Status.SUBMITTED;

        @Column(name = "processed_by_admin_id")
        private Long processedByAdminId;

        @Column(name = "processed_at")
        private OffsetDateTime processedAt;

        @Column(name = "rejection_reason", columnDefinition = "TEXT")
        private String rejectionReason;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private OffsetDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private OffsetDateTime updatedAt;

        @Builder
        private ExpoChangeRequest(
                Long expoId,
                Long requesterClientId,
                String changeReason,
                Map<String, Object> requestedChanges) {
            this.expoId = expoId;
            this.requesterClientId = requesterClientId;
            this.changeReason = changeReason;
            this.requestedChanges = requestedChanges;
            this.status = Status.SUBMITTED;
        }
    }
}
