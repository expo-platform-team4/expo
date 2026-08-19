package com.expo.expo.repository;

import com.expo.expo.entity.ExpoAttachments.ExpoImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpoImageRepository extends JpaRepository<ExpoImage, Long> {
    List<ExpoImage> findByExpoIdOrderBySortOrderAscIdAsc(Long expoId);
}
