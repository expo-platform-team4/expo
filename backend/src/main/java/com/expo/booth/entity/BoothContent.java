package com.expo.booth.entity;

import com.expo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 확정 배정된 참여 기업이 관리하는 기업·부스 소개 콘텐츠. */
@Getter
@Entity
@Table(name = "booth_contents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoothContent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booth_allocation_id", nullable = false, unique = true)
    private Long boothAllocationId;

    @Column(name = "client_user_id", nullable = false)
    private Long clientUserId;

    @Column(name = "company_display_name", nullable = false, length = 150)
    private String companyDisplayName;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "company_description", columnDefinition = "TEXT")
    private String companyDescription;

    @Column(name = "booth_description", columnDefinition = "TEXT")
    private String boothDescription;

    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;

    @Column(name = "logo_file_id")
    private Long logoFileId;

    @Column(name = "main_image_file_id")
    private Long mainImageFileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BoothContentStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "correction_requested_at")
    private Instant correctionRequestedAt;

    @Column(name = "correction_message", columnDefinition = "TEXT")
    private String correctionMessage;

    @Column(name = "checked_by_admin_id")
    private Long checkedByAdminId;

    @Column(name = "checked_at")
    private Instant checkedAt;
}
