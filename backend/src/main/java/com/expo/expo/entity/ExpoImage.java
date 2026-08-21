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
 * 박람회에 붙인 이미지 한 장 ({@code expo_images}).
 *
 * <p>이미지 <b>파일</b>은 {@code file_metadata} 에 있고 여기는 "그 파일을 이 박람회의 무엇으로 쓴다" 는 연결만 담는다. 같은 파일을 두 번
 * 붙일 수 없다 — {@code uq_expo_images_file (expo_id, file_id)}.
 */
@Getter
@Entity
@Table(name = "expo_images")
public class ExpoImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expo_id", nullable = false)
    private Long expoId;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 20)
    private ExpoImageType imageType;

    /** 대체 텍스트. 화면 낭독기가 읽고, 이미지가 깨졌을 때도 보인다. */
    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    protected ExpoImage() {}

    public static ExpoImage attach(
            Long expoId, Long fileId, ExpoImageType imageType, String altText, int sortOrder) {
        ExpoImage image = new ExpoImage();
        image.expoId = expoId;
        image.fileId = fileId;
        image.imageType = imageType;
        image.altText = altText;
        image.sortOrder = sortOrder;
        return image;
    }

    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /** 대표 이미지를 새로 지정할 때 기존 대표를 상세로 내린다 — 지우면 주최사가 올린 사진이 사라진다. */
    public void demoteToDetail() {
        this.imageType = ExpoImageType.DETAIL;
    }

    public boolean isThumbnail() {
        return imageType == ExpoImageType.THUMBNAIL;
    }

    public boolean belongsTo(Long expoId) {
        return this.expoId.equals(expoId);
    }
}
