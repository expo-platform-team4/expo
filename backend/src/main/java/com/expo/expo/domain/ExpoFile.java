package com.expo.expo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 박람회 첨부 자료(EXPO_FILE) 엔티티
 *
 * ※ 테이블 정의서 v2에는 대표 이미지(thumbnail_url) 컬럼만 존재하므로,
 *   희-EXPO-13(외부 링크) / 희-EXPO-14(PDF·이미지·홍보영상) / 희-EXPO-15(카탈로그·리플렛)
 *   기능 지원을 위해 1:N 보조 테이블로 확장한 것. (정의서 외 추가 테이블)
 */
@Entity
@Table(
    name = "expo_file",
    indexes = @Index(name = "idx_expo_file_expo", columnList = "expo_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpoFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expo_file_id")
    private Long expoFileId;

    /** 소속 박람회 — FK(expo.expo_id) */
    @Column(name = "expo_id", nullable = false)
    private Long expoId;

    /** 자료 유형 (EXTERNAL_LINK / PDF / IMAGE / VIDEO / CATALOG / LEAFLET) */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 30, nullable = false)
    private ExpoFileType fileType;

    /** 파일 저장 경로 또는 외부 링크 URL */
    @Column(name = "url", length = 1000, nullable = false)
    private String url;

    /** 원본 파일명 또는 링크 표시명 */
    @Column(name = "display_name", length = 300)
    private String displayName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private ExpoFile(Long expoId, ExpoFileType fileType, String url, String displayName) {
        this.expoId = expoId;
        this.fileType = fileType;
        this.url = url;
        this.displayName = displayName;
    }
}
