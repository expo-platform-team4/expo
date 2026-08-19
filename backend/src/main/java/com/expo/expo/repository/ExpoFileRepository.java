package com.expo.expo.repository;

import com.expo.expo.entity.ExpoAttachments.ExpoFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpoFileRepository extends JpaRepository<ExpoFile, Long> {
    List<ExpoFile> findByExpoIdOrderBySortOrderAscIdAsc(Long expoId);
}
