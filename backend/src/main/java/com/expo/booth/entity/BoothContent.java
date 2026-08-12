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

    /** 확정 배정 기업의 부스 콘텐츠 작성. 상태는 DRAFT 로 고정한다. */
    public static BoothContent create(
            Long boothAllocationId,
            Long clientUserId,
            String companyDisplayName,
            String title,
            String companyDescription,
            String boothDescription,
            String productDescription) {
        BoothContent content = new BoothContent();
        content.boothAllocationId = boothAllocationId;
        content.clientUserId = clientUserId;
        content.companyDisplayName = companyDisplayName;
        content.title = title;
        content.companyDescription = companyDescription;
        content.boothDescription = boothDescription;
        content.productDescription = productDescription;
        content.status = BoothContentStatus.DRAFT;
        return content;
    }

    /** 로고·대표 이미지 첨부. {@link #create} 뒤에 이어서 호출한다. */
    public BoothContent attachImages(Long logoFileId, Long mainImageFileId) {
        this.logoFileId = logoFileId;
        this.mainImageFileId = mainImageFileId;
        return this;
    }

    /** 콘텐츠 본문 수정. 상태 전이는 하지 않는다. */
    public void updateContent(
            String companyDisplayName,
            String title,
            String companyDescription,
            String boothDescription,
            String productDescription,
            Long logoFileId,
            Long mainImageFileId) {
        this.companyDisplayName = companyDisplayName;
        this.title = title;
        this.companyDescription = companyDescription;
        this.boothDescription = boothDescription;
        this.productDescription = productDescription;
        this.logoFileId = logoFileId;
        this.mainImageFileId = mainImageFileId;
    }

    /** 콘텐츠 공개. */
    public void publish() {
        this.status = BoothContentStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    /** 관리자 운영 확인. 상태는 바꾸지 않고 확인 시각·주체만 기록한다. */
    public void check(Long adminId) {
        this.checkedByAdminId = adminId;
        this.checkedAt = Instant.now();
    }

    /** 보완 요청. 공개를 내리고 사유를 남긴다. */
    public void requestCorrection(String message) {
        this.status = BoothContentStatus.CORRECTION_REQUESTED;
        this.correctionRequestedAt = Instant.now();
        this.correctionMessage = message;
    }

    /** 관리자 직권 숨김. */
    public void hide() {
        this.status = BoothContentStatus.HIDDEN;
    }

    /** 숨김 해제. 공개 상태로 복원한다. */
    public void restore() {
        this.status = BoothContentStatus.PUBLISHED;
    }
}
