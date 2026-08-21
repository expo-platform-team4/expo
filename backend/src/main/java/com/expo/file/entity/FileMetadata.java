package com.expo.file.entity;

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
 * 업로드된 파일 한 건의 저장소 위치와 상태 ({@code file_metadata}).
 *
 * <p>테이블은 V1 부터 있었지만 참조하는 자바 코드가 없었다(이슈 #93). <b>11개 테이블이 FK 12개로 이 테이블을 가리킨다</b> — 프로필 이미지,
 * 박람회 이미지·자료, 부스 콘텐츠, 배너, 장소 배치도, 정산 리포트. 그래서 도메인마다 따로 만들지 않고 여기 하나만 둔다.
 *
 * <p>파일 <b>내용</b>은 여기 없다. 실제 바이트는 객체 저장소에 있고 이 행은 그 위치({@code bucketName} + {@code storageKey})와
 * 판정에 필요한 정보만 갖는다.
 */
@Getter
@Entity
@Table(name = "file_metadata")
public class FileMetadata extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 업로드한 사용자. 비공개 파일의 소유자 판정 기준이다. */
    @Column(name = "uploader_user_id")
    private Long uploaderUserId;

    /** 저장소 종류. {@code S3} 처럼 구현이 스스로 보고한 값을 그대로 담는다. */
    @Column(name = "storage_provider", nullable = false, length = 20)
    private String storageProvider;

    @Column(name = "bucket_name", nullable = false, length = 100)
    private String bucketName;

    /** 객체 키. UNIQUE 라 같은 키로 두 번 저장되지 않는다. */
    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /** 내용의 SHA-256 (hex 64자). 컬럼은 128자라 여유가 있다. */
    @Column(length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_status", nullable = false, length = 20)
    private FileStatus fileStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 20)
    private FileAccessLevel accessLevel;

    protected FileMetadata() {}

    /**
     * 저장소에 객체를 올린 <b>뒤</b> 그 결과를 기록한다.
     *
     * <p>순서가 중요하다. 행을 먼저 만들면 업로드가 실패했을 때 가리킬 객체가 없는 행이 남는다. 반대로 하면 최악이 참조되지 않는 객체 하나이고, 그건
     * 정리 배치가 지울 수 있다.
     */
    public static FileMetadata record(
            Long uploaderUserId, StoredLocation location, UploadedContent content) {
        FileMetadata metadata = new FileMetadata();
        metadata.uploaderUserId = uploaderUserId;
        metadata.storageProvider = location.provider();
        metadata.bucketName = location.bucket();
        metadata.storageKey = location.key();
        metadata.originalFilename = content.originalFilename();
        metadata.contentType = content.contentType();
        metadata.fileSize = content.size();
        metadata.checksum = content.checksum();
        metadata.fileStatus = FileStatus.ACTIVE;
        metadata.accessLevel = content.accessLevel();
        return metadata;
    }

    /** 논리 삭제. 저장소의 실제 객체는 지우지 않는다 — 다른 행이 같은 파일을 참조할 수 있다. */
    public void markDeleted() {
        this.fileStatus = FileStatus.DELETED;
    }

    /** 내줄 수 있는 상태인가. 삭제·격리된 파일은 존재 자체를 알리지 않는다. */
    public boolean isDownloadable() {
        return fileStatus == FileStatus.ACTIVE;
    }

    public boolean isPublic() {
        return accessLevel == FileAccessLevel.PUBLIC;
    }

    public boolean isUploadedBy(Long userId) {
        return userId != null && userId.equals(uploaderUserId);
    }
}
