package com.expo.expo.entity;

import com.expo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * 박람회에 붙인 자료 한 건 ({@code expo_files}) — 소개 PDF·카탈로그·리플렛·홍보영상.
 *
 * <p>{@link ExpoImage} 와 같은 구조다. 파일 자체는 {@code file_metadata} 에 있고 여기는 연결만 담는다.
 */
@Getter
@Entity
@Table(name = "expo_files")
public class ExpoFile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expo_id", nullable = false)
    private Long expoId;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_purpose", nullable = false, length = 30)
    private ExpoFilePurpose filePurpose;

    /** 화면에 보여 줄 이름. 비우면 원본 파일명을 쓴다. */
    @Column(length = 150)
    private String title;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    protected ExpoFile() {}

    public static ExpoFile attach(
            Long expoId, Long fileId, ExpoFilePurpose filePurpose, String title, int sortOrder) {
        ExpoFile file = new ExpoFile();
        file.expoId = expoId;
        file.fileId = fileId;
        file.filePurpose = filePurpose;
        file.title = title;
        file.sortOrder = sortOrder;
        return file;
    }

    public boolean belongsTo(Long expoId) {
        return this.expoId.equals(expoId);
    }
}
