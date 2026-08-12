package com.expo.booth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 부스 콘텐츠의 갤러리 이미지·영상·카탈로그·리플렛 파일.
 *
 * <p>{@code created_at} 만 있고 {@code updated_at} 이 없는 append-only 테이블이라 {@code BaseTimeEntity} 를
 * 상속하지 않는다.
 */
@Getter
@Entity
@Table(name = "booth_content_files")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoothContentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booth_content_id", nullable = false)
    private Long boothContentId;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 30)
    private BoothContentFileType fileType;

    @Column(length = 150)
    private String title;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 부스 콘텐츠 첨부 파일 등록. */
    public static BoothContentFile create(
            Long boothContentId,
            Long fileId,
            BoothContentFileType fileType,
            String title,
            Integer sortOrder) {
        BoothContentFile file = new BoothContentFile();
        file.boothContentId = boothContentId;
        file.fileId = fileId;
        file.fileType = fileType;
        file.title = title;
        file.sortOrder = sortOrder != null ? sortOrder : 0;
        return file;
    }

    /** 첨부 파일 노출 순서 변경. */
    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
