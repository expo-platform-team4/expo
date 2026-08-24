package com.expo.expo.repository;

import com.expo.expo.entity.ExpoAttachments.ExpoFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** {@code expo_files} 조회. */
public interface ExpoFileRepository extends JpaRepository<ExpoFile, Long> {

    List<ExpoFile> findByExpoIdOrderBySortOrderAscIdAsc(Long expoId);

    boolean existsByExpoIdAndFileId(Long expoId, Long fileId);

    @Query("select max(f.sortOrder) from ExpoFile f where f.expoId = :expoId")
    Integer findMaxSortOrder(Long expoId);
}
