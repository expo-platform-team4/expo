package com.expo.expo.repository;

import com.expo.expo.domain.ExpoFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpoFileRepository extends JpaRepository<ExpoFile, Long> {

    List<ExpoFile> findByExpoIdOrderByCreatedAtAsc(Long expoId);
}
