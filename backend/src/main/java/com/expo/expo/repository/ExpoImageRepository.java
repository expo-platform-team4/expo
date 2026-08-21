package com.expo.expo.repository;

import com.expo.expo.entity.ExpoImage;
import com.expo.expo.entity.ExpoImageType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** {@code expo_images} 조회. */
public interface ExpoImageRepository extends JpaRepository<ExpoImage, Long> {

    List<ExpoImage> findByExpoIdOrderBySortOrderAscIdAsc(Long expoId);

    List<ExpoImage> findByExpoIdAndImageType(Long expoId, ExpoImageType imageType);

    boolean existsByExpoIdAndFileId(Long expoId, Long fileId);

    /** 새로 붙일 이미지의 순서를 뒤에 놓기 위한 현재 최댓값. 아직 없으면 {@code null} 이다. */
    @Query("select max(i.sortOrder) from ExpoImage i where i.expoId = :expoId")
    Integer findMaxSortOrder(Long expoId);
}
